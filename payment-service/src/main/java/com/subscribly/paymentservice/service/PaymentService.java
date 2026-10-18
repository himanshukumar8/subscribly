package com.subscribly.paymentservice.service;

import com.subscribly.common.dto.SubscriptionDto;
import com.subscribly.common.dto.UserDto;
import com.subscribly.common.event.PaymentCompletedEvent;
import com.subscribly.common.event.PaymentFailedEvent;
import com.subscribly.paymentservice.client.SubscriptionClient;
import com.subscribly.paymentservice.client.UserClient;
import com.subscribly.paymentservice.dto.*;
import com.subscribly.paymentservice.entity.Payment;
import com.subscribly.paymentservice.entity.PaymentAuditLog;
import com.subscribly.paymentservice.entity.PaymentStatus;
import com.subscribly.paymentservice.entity.WebhookEventRecord;
import com.subscribly.paymentservice.gateway.GatewayOrderResponse;
import com.subscribly.paymentservice.gateway.PaymentGatewayProvider;
import com.subscribly.paymentservice.repository.PaymentAuditLogRepository;
import com.subscribly.paymentservice.repository.PaymentRepository;
import com.subscribly.paymentservice.repository.WebhookEventRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentAuditLogRepository auditLogRepository;
    private final WebhookEventRecordRepository webhookEventRecordRepository;
    private final PaymentGatewayProvider gatewayProvider;
    private final PaymentStateMachine stateMachine;
    private final PaymentAuditService auditService;
    private final UserClient userClient;
    private final SubscriptionClient subscriptionClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "payment-events";

    @Transactional
    public PaymentResponse createOrder(String email, CreateOrderRequest request) {
        log.info("[PaymentService] Creating payment order for user: {}, subscription: {}", email, request.getSubscriptionId());

        UserDto userDto = userClient.getUserByEmail(email);
        if (userDto == null) {
            throw new RuntimeException("User not found: " + email);
        }

        SubscriptionDto subscriptionDto = subscriptionClient.getSubscriptionById(request.getSubscriptionId());
        if (subscriptionDto == null) {
            throw new RuntimeException("Subscription not found: " + request.getSubscriptionId());
        }

        if (!subscriptionDto.getUserId().equals(userDto.getId())) {
            throw new RuntimeException("Unauthorized: Subscription does not belong to user");
        }

        Double amount = request.getAmount() != null ? request.getAmount() : subscriptionDto.getPrice();
        String currency = request.getCurrency() != null ? request.getCurrency() : "INR";
        String paymentReference = "PAY-REF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        // 1. Invoke Gateway via Abstraction
        GatewayOrderResponse gatewayOrder = gatewayProvider.createOrder(paymentReference, amount, currency);

        // 2. Persist initial payment state
        Payment payment = Payment.builder()
                .paymentReference(paymentReference)
                .gatewayOrderId(gatewayOrder.orderId())
                .subscriptionId(subscriptionDto.getId())
                .userId(userDto.getId())
                .amount(amount)
                .currency(currency)
                .status(PaymentStatus.CREATED)
                .build();

        Payment saved = paymentRepository.save(payment);

        // 3. Immutable Audit Logging
        auditService.recordTransition(
                saved.getId(),
                saved.getPaymentReference(),
                null,
                PaymentStatus.CREATED,
                "Payment order created on gateway " + gatewayProvider.getProviderName(),
                "USER"
        );

        return toResponse(saved);
    }

    @Transactional
    public PaymentResponse verifyPayment(String email, VerifyPaymentRequest request) {
        log.info("[PaymentService] Verifying payment for Gateway Order ID: {}", request.getOrderId());

        Payment payment = paymentRepository.findByGatewayOrderId(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Payment order not found: " + request.getOrderId()));

        // Idempotency check: if already SUCCESS, return immediately
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("[PaymentService] Payment {} is already SUCCESS. Returning idempotent response.", payment.getPaymentReference());
            return toResponse(payment);
        }

        boolean signatureValid = gatewayProvider.verifyPaymentSignature(
                request.getOrderId(),
                request.getPaymentId(),
                request.getSignature()
        );

        PaymentStatus previousStatus = payment.getStatus();

        if (signatureValid) {
            stateMachine.validateTransition(previousStatus, PaymentStatus.SUCCESS);
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setGatewayPaymentId(request.getPaymentId());
            payment.setGatewaySignature(request.getSignature());
            payment.setFailureReason(null);
            Payment updated = paymentRepository.save(payment);

            auditService.recordTransition(
                    updated.getId(),
                    updated.getPaymentReference(),
                    previousStatus,
                    PaymentStatus.SUCCESS,
                    "Payment signature verified successfully",
                    "USER"
            );

            // Publish Kafka Event
            PaymentCompletedEvent event = new PaymentCompletedEvent(
                    updated.getId(),
                    updated.getSubscriptionId(),
                    updated.getUserId(),
                    updated.getAmount(),
                    updated.getPaymentReference()
            );
            publishEventSafely(String.valueOf(updated.getId()), event);

            return toResponse(updated);
        } else {
            // Signature verification failed -> transition to FAILED with retry strategy
            stateMachine.validateTransition(previousStatus, PaymentStatus.FAILED);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setRetryCount(payment.getRetryCount() + 1);
            payment.setFailureReason("Signature verification failed");
            payment.setNextRetryAt(calculateNextRetry(payment.getRetryCount()));
            Payment updated = paymentRepository.save(payment);

            auditService.recordTransition(
                    updated.getId(),
                    updated.getPaymentReference(),
                    previousStatus,
                    PaymentStatus.FAILED,
                    "Payment verification signature mismatch",
                    "USER"
            );

            // Publish Kafka Event
            PaymentFailedEvent event = new PaymentFailedEvent(
                    updated.getId(),
                    updated.getSubscriptionId(),
                    updated.getUserId(),
                    updated.getAmount(),
                    "Signature verification failed"
            );
            publishEventSafely(String.valueOf(updated.getId()), event);

            return toResponse(updated);
        }
    }

    @Transactional
    public boolean processWebhook(String payload, String signatureHeader, WebhookPayload webhookData) {
        if (webhookData == null) {
            log.warn("[PaymentService] Webhook payload could not be parsed");
            return false;
        }
        if (webhookData.getEventId() == null || webhookData.getEventId().isBlank()) {
            webhookData.setEventId("evt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        }
        log.info("[PaymentService] Processing webhook event: {}", webhookData.getEventId());

        // 1. Idempotency Protection: check if event was already processed
        if (webhookEventRecordRepository.existsByEventId(webhookData.getEventId())) {
            log.warn("[PaymentService] Duplicate webhook event ignored (idempotent check): {}", webhookData.getEventId());
            return true;
        }

        // 2. Verify Webhook Signature
        if (!gatewayProvider.verifyWebhookSignature(payload, signatureHeader)) {
            log.error("[PaymentService] Webhook signature verification failed for event: {}", webhookData.getEventId());
            throw new SecurityException("Invalid webhook signature header");
        }

        // 3. Record webhook event for idempotency
        WebhookEventRecord record = WebhookEventRecord.builder()
                .eventId(webhookData.getEventId())
                .eventType(webhookData.getEventType())
                .processedAt(LocalDateTime.now())
                .status("PROCESSED")
                .build();
        webhookEventRecordRepository.save(record);

        // 4. Update state machine
        Payment payment = paymentRepository.findByGatewayOrderId(webhookData.getOrderId())
                .orElse(null);

        if (payment == null) {
            log.warn("[PaymentService] Payment not found for webhook order ID: {}", webhookData.getOrderId());
            return true;
        }

        PaymentStatus previousStatus = payment.getStatus();
        String eventType = webhookData.getEventType() != null ? webhookData.getEventType() : "";

        if (eventType.contains("captured") || eventType.contains("authorized") || eventType.contains("success")) {
            if (previousStatus != PaymentStatus.SUCCESS) {
                stateMachine.validateTransition(previousStatus, PaymentStatus.SUCCESS);
                payment.setStatus(PaymentStatus.SUCCESS);
                if (webhookData.getPaymentId() != null) {
                    payment.setGatewayPaymentId(webhookData.getPaymentId());
                }
                Payment updated = paymentRepository.save(payment);

                auditService.recordTransition(
                        updated.getId(),
                        updated.getPaymentReference(),
                        previousStatus,
                        PaymentStatus.SUCCESS,
                        "Webhook event: " + eventType,
                        "WEBHOOK"
                );

                PaymentCompletedEvent event = new PaymentCompletedEvent(
                        updated.getId(),
                        updated.getSubscriptionId(),
                        updated.getUserId(),
                        updated.getAmount(),
                        updated.getPaymentReference()
                );
                publishEventSafely(String.valueOf(updated.getId()), event);
            }
        } else if (eventType.contains("failed")) {
            stateMachine.validateTransition(previousStatus, PaymentStatus.FAILED);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setRetryCount(payment.getRetryCount() + 1);
            payment.setFailureReason(webhookData.getFailureReason() != null ? webhookData.getFailureReason() : "Webhook failure notification");
            payment.setNextRetryAt(calculateNextRetry(payment.getRetryCount()));
            Payment updated = paymentRepository.save(payment);

            auditService.recordTransition(
                    updated.getId(),
                    updated.getPaymentReference(),
                    previousStatus,
                    PaymentStatus.FAILED,
                    "Webhook failure event: " + eventType,
                    "WEBHOOK"
            );

            PaymentFailedEvent event = new PaymentFailedEvent(
                    updated.getId(),
                    updated.getSubscriptionId(),
                    updated.getUserId(),
                    updated.getAmount(),
                    payment.getFailureReason()
            );
            publishEventSafely(String.valueOf(updated.getId()), event);
        }

        return true;
    }

    public List<PaymentResponse> getMyPayments(String email) {
        UserDto user = userClient.getUserByEmail(email);
        if (user == null) {
            return List.of();
        }
        return paymentRepository.findByUserId(user.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<PaymentAuditLog> getAuditLogs(Long paymentId) {
        return auditLogRepository.findByPaymentIdOrderByTimestampAsc(paymentId);
    }

    private LocalDateTime calculateNextRetry(int retryCount) {
        // Exponential backoff model: 15m, 60m, 360m, etc.
        long delayMinutes = (long) (15 * Math.pow(2, Math.min(retryCount - 1, 4)));
        return LocalDateTime.now().plusMinutes(delayMinutes);
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .paymentReference(p.getPaymentReference())
                .gatewayOrderId(p.getGatewayOrderId())
                .gatewayPaymentId(p.getGatewayPaymentId())
                .subscriptionId(p.getSubscriptionId())
                .userId(p.getUserId())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .status(p.getStatus())
                .retryCount(p.getRetryCount())
                .nextRetryAt(p.getNextRetryAt())
                .failureReason(p.getFailureReason())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private void publishEventSafely(String key, Object event) {
        try {
            kafkaTemplate.send(TOPIC, key, event);
            log.info("[PaymentService] Published event to topic: {}, key: {}", TOPIC, key);
        } catch (Exception e) {
            log.warn("[PaymentService] Could not publish event to Kafka (broker unreachable or timeout): {}", e.getMessage());
        }
    }
}
