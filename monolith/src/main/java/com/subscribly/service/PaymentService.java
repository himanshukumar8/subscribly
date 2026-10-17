package com.subscribly.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subscribly.dto.CreatePaymentOrderResponse;
import com.subscribly.dto.WebhookPayload;
import com.subscribly.entity.Payment;
import com.subscribly.entity.PaymentStatus;
import com.subscribly.entity.Subscription;
import com.subscribly.entity.SubscriptionStatus;
import com.subscribly.entity.WebhookEvent;
import com.subscribly.gateway.GatewayOrderResponse;
import com.subscribly.gateway.PaymentGatewayProvider;
import com.subscribly.repository.PaymentRepository;
import com.subscribly.repository.SubscriptionRepository;
import com.subscribly.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    public static final int MAX_RETRIES = 3;
    public static final Duration DEFAULT_BASE_BACKOFF = Duration.ofHours(1);

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentGatewayProvider gatewayProvider;
    private final WebhookEventService webhookEventService;
    private final ObjectMapper objectMapper;

    /**
     * Calculates exponential backoff duration based on attempt number:
     * Attempt 1 -> baseInterval * 1 (2^0)
     * Attempt 2 -> baseInterval * 2 (2^1)
     * Attempt 3 -> baseInterval * 4 (2^2)
     */
    public Duration calculateBackoff(int attempt, Duration baseInterval) {
        if (baseInterval == null) {
            baseInterval = DEFAULT_BASE_BACKOFF;
        }
        int shift = Math.max(0, Math.min(attempt - 1, 30));
        long multiplier = 1L << shift;
        return baseInterval.multipliedBy(multiplier);
    }

    /**
     * USER: Make manual payment with idempotency support.
     * Prevents double billing and duplicate subscription extensions.
     */
    @Transactional
    public Payment makePayment(Long subscriptionId, String email) {
        return makePayment(subscriptionId, email, null);
    }

    @Transactional
    public Payment makePayment(Long subscriptionId, String email, String idempotencyKey) {
        return processPaymentAttempt(subscriptionId, email, idempotencyKey, true, null, DEFAULT_BASE_BACKOFF);
    }

    /**
     * Core payment execution with idempotency, state verification, and exponential retry handling.
     */
    @Transactional
    public Payment processPaymentAttempt(
            Long subscriptionId,
            String email,
            String idempotencyKey,
            boolean simulateSuccess,
            String failureReason,
            Duration baseBackoff
    ) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (!subscription.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new SecurityException("Unauthorized: Subscription does not belong to user");
        }

        // Cancelled subscriptions cannot be renewed or paid manually
        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            log.warn("[PaymentService] Payment attempted on CANCELLED subscription id={}", subscriptionId);
            throw new IllegalStateException("Cannot process payment for a CANCELLED subscription.");
        }

        Instant now = Instant.now();

        // Generate fallback idempotency key if none supplied
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = "manual-" + subscriptionId + "-" + now.getEpochSecond();
        }

        // Idempotency check: verify if reference already processed
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            Payment p = existing.get();
            // Validate payload consistency (subscriptionId, user email, amount)
            boolean subMatches = p.getSubscription() != null && p.getSubscription().getId().equals(subscriptionId);
            boolean userMatches = p.getUser() != null && p.getUser().getEmail().equalsIgnoreCase(email);
            boolean amountMatches = p.getAmount() != null && Math.abs(p.getAmount() - subscription.getPrice()) < 0.001;

            if (!subMatches || !userMatches || !amountMatches) {
                log.error("[PaymentService] Idempotency key conflict: key={} already used with different parameters.", idempotencyKey);
                throw new IllegalStateException("Idempotency key '" + idempotencyKey + "' was already used with different request parameters.");
            }

            log.info("[PaymentService] Idempotent hit: reference={} already processed with status={}. Returning without re-extending.",
                    idempotencyKey, p.getStatus());
            return p;
        }

        try {
            if (simulateSuccess) {
                // Successful payment: grant entitlement and advance expiry
                extendSubscriptionDuration(subscription, now);

                Payment payment = Payment.builder()
                        .subscription(subscription)
                        .user(subscription.getUser())
                        .idempotencyKey(idempotencyKey)
                        .amount(subscription.getPrice())
                        .status(PaymentStatus.SUCCESS)
                        .attempts(1)
                        .createdAt(LocalDate.ofInstant(now, ZoneOffset.UTC))
                        .createdAtInstant(now)
                        .processedAtInstant(now)
                        .nextRetryAt(null)
                        .nextRetryInstant(null)
                        .autoRenew(false)
                        .build();

                Payment saved = paymentRepository.save(payment);
                log.info("[PaymentService] Payment successful: id={}, idempotencyKey={}, new expiry={}",
                        saved.getId(), idempotencyKey, subscription.getExpiresAt());
                return saved;
            } else {
                // Failed payment: schedule bounded retry with exponential backoff.
                // Critical: Failed or unverified payments NEVER grant entitlement or extend duration.
                Duration backoff = calculateBackoff(1, baseBackoff);
                Instant nextRetry = now.plus(backoff);

                Payment payment = Payment.builder()
                        .subscription(subscription)
                        .user(subscription.getUser())
                        .idempotencyKey(idempotencyKey)
                        .amount(subscription.getPrice())
                        .status(PaymentStatus.RETRYING)
                        .attempts(1)
                        .createdAt(LocalDate.ofInstant(now, ZoneOffset.UTC))
                        .createdAtInstant(now)
                        .nextRetryAt(LocalDate.ofInstant(nextRetry, ZoneOffset.UTC))
                        .nextRetryInstant(nextRetry)
                        .failureReason(failureReason != null ? failureReason : "Initial payment attempt failed")
                        .autoRenew(false)
                        .build();

                Payment saved = paymentRepository.save(payment);
                log.warn("[PaymentService] Payment attempt 1 failed: id={}, idempotencyKey={}, nextRetryAt={}",
                        saved.getId(), idempotencyKey, nextRetry);
                return saved;
            }
        } catch (DataIntegrityViolationException ex) {
            // Concurrency safety: handle unique constraint collision gracefully
            log.info("[PaymentService] Concurrent duplicate detected for idempotencyKey={}. Recovering existing record.", idempotencyKey);
            return paymentRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> ex);
        }
    }

    /**
     * Retries a failed or retrying payment with bounded exponential backoff.
     * When MAX_RETRIES is exhausted, transitions to FAILED and revokes active status.
     */
    @Transactional
    public Payment retryPayment(
            Payment payment,
            Instant now,
            boolean simulateSuccess,
            String failureReason,
            Duration baseBackoff
    ) {
        // Reload fresh persistent state to prevent duplicate processing by concurrent workers
        Payment currentPayment = paymentRepository.findById(payment.getId()).orElse(payment);

        if (currentPayment.getStatus() != PaymentStatus.RETRYING && currentPayment.getStatus() != PaymentStatus.PENDING) {
            log.info("[PaymentService] Payment id={} status is {}, skipping duplicate retry.",
                    currentPayment.getId(), currentPayment.getStatus());
            return currentPayment;
        }

        Subscription subscription = currentPayment.getSubscription();
        int attempt = currentPayment.getAttempts() + 1;
        currentPayment.setAttempts(attempt);

        if (simulateSuccess) {
            // Succeeded on retry: grant entitlement
            extendSubscriptionDuration(subscription, now);

            currentPayment.setStatus(PaymentStatus.SUCCESS);
            currentPayment.setProcessedAtInstant(now);
            currentPayment.setNextRetryAt(null);
            currentPayment.setNextRetryInstant(null);
            currentPayment.setFailureReason(null);

            log.info("[PaymentService] Payment retry succeeded on attempt {} for payment id={}", attempt, currentPayment.getId());
            return paymentRepository.save(currentPayment);
        } else {
            // Failed retry attempt
            if (attempt < MAX_RETRIES) {
                Duration backoff = calculateBackoff(attempt, baseBackoff);
                Instant nextRetry = now.plus(backoff);

                currentPayment.setStatus(PaymentStatus.RETRYING);
                currentPayment.setNextRetryInstant(nextRetry);
                currentPayment.setNextRetryAt(LocalDate.ofInstant(nextRetry, ZoneOffset.UTC));
                currentPayment.setFailureReason(failureReason != null ? failureReason : "Retry attempt " + attempt + " failed");

                log.warn("[PaymentService] Payment retry attempt {} failed for id={}, nextRetry={}",
                        attempt, currentPayment.getId(), nextRetry);
            } else {
                // Bounded retries exhausted: mark FAILED and revoke entitlement
                currentPayment.setStatus(PaymentStatus.FAILED);
                currentPayment.setNextRetryInstant(null);
                currentPayment.setNextRetryAt(null);
                currentPayment.setFailureReason("Retry exhausted after " + MAX_RETRIES + " attempts. Last failure: " + failureReason);

                // If subscription has reached or passed expiry, transition status to EXPIRED
                if (subscription.isExpired(now)) {
                    subscription.setStatus(SubscriptionStatus.EXPIRED);
                    subscriptionRepository.save(subscription);
                }

                log.error("[PaymentService] Payment id={} reached MAX_RETRIES ({}). Marked FAILED, entitlement revoked.",
                        currentPayment.getId(), MAX_RETRIES);
            }
            return paymentRepository.save(currentPayment);
        }
    }

    /**
     * GATEWAY TRUST BOUNDARY: Record a cryptographically verified payment from a gateway webhook or callback.
     * Enforces amount match, user ownership, and at-most-once state transition.
     */
    @Transactional
    public Payment recordVerifiedGatewayPayment(
            String transactionReference,
            Long subscriptionId,
            String email,
            double amount,
            Instant processedAt
    ) {
        if (transactionReference == null || transactionReference.isBlank()) {
            throw new IllegalArgumentException("Gateway transaction reference is required");
        }

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (!subscription.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new SecurityException("Unauthorized: Subscription does not belong to user");
        }

        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new IllegalStateException("Cannot process payment for a CANCELLED subscription.");
        }

        if (Math.abs(subscription.getPrice() - amount) > 0.001) {
            log.error("[PaymentService] Gateway payment amount mismatch: expected={}, received={}",
                    subscription.getPrice(), amount);
            throw new IllegalArgumentException("Verified amount does not match subscription price");
        }

        Instant now = processedAt != null ? processedAt : Instant.now();

        // Idempotency check for gateway event
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(transactionReference);
        if (existing.isPresent()) {
            Payment p = existing.get();
            log.info("[PaymentService] Gateway event reference={} already processed with status={}.",
                    transactionReference, p.getStatus());
            return p;
        }

        extendSubscriptionDuration(subscription, now);

        Payment payment = Payment.builder()
                .subscription(subscription)
                .user(subscription.getUser())
                .idempotencyKey(transactionReference)
                .amount(amount)
                .status(PaymentStatus.SUCCESS)
                .attempts(1)
                .createdAt(LocalDate.ofInstant(now, ZoneOffset.UTC))
                .createdAtInstant(now)
                .processedAtInstant(now)
                .autoRenew(false)
                .build();

        return paymentRepository.save(payment);
    }

    /**
     * Extends subscription duration safely and synchronizes timestamp and date boundaries.
     */
    private void extendSubscriptionDuration(Subscription subscription, Instant now) {
        Instant baseTime = subscription.getEffectiveExpiryInstant() != null && subscription.getEffectiveExpiryInstant().isAfter(now)
                ? subscription.getEffectiveExpiryInstant()
                : now;

        int months = subscription.getBillingCycle() != null ? subscription.getBillingCycle().getMonths() : 1;
        Instant newExpiry = baseTime.atZone(ZoneOffset.UTC).plusMonths(months).toInstant();

        subscription.setExpiresAt(newExpiry);
        subscription.setEndDate(LocalDate.ofInstant(newExpiry, ZoneOffset.UTC));
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(subscription);
    }

    /**
     * Process due retries in micro-batches using exact timestamp checks.
     */
    @Transactional
    public int processDueRetries(Instant now, Pageable limit) {
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        List<Payment> duePayments = paymentRepository.findPaymentsToProcessInstant(
                PaymentStatus.RETRYING, PaymentStatus.PENDING, now, today, limit);

        int count = 0;
        for (Payment payment : duePayments) {
            // Process payment retry (real gateway invocation in production, simulation here)
            retryPayment(payment, now, true, null, DEFAULT_BASE_BACKOFF);
            count++;
        }
        return count;
    }

    /**
     * GATEWAY ORDER CREATION: Initiates server-side payment order with sandbox provider.
     * Generates gatewayOrderId, initializes payment record in PENDING state, and prevents duplicates.
     */
    @Transactional
    public CreatePaymentOrderResponse createPaymentOrder(
            Long subscriptionId,
            String email,
            String currency,
            String idempotencyKey
    ) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (!subscription.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new SecurityException("Unauthorized: Subscription does not belong to user");
        }

        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            log.warn("[PaymentService] Cannot create order for CANCELLED subscription id={}", subscriptionId);
            throw new IllegalStateException("Cannot create payment order for a CANCELLED subscription.");
        }

        if (currency == null || currency.isBlank()) {
            currency = "INR";
        } else {
            currency = currency.trim().toUpperCase(Locale.ROOT);
        }

        Instant now = Instant.now();
        String paymentReference = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey
                : "PAY-REF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        // Idempotency: verify if reference already created
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(paymentReference);
        if (existing.isPresent()) {
            Payment p = existing.get();
            // Validate payload consistency (subscriptionId, user email, amount, currency)
            boolean subMatches = p.getSubscription() != null && p.getSubscription().getId().equals(subscriptionId);
            boolean userMatches = p.getUser() != null && p.getUser().getEmail().equalsIgnoreCase(email);
            boolean amountMatches = p.getAmount() != null && Math.abs(p.getAmount() - subscription.getPrice()) < 0.001;
            boolean currencyMatches = p.getCurrency() != null && p.getCurrency().equalsIgnoreCase(currency);

            if (!subMatches || !userMatches || !amountMatches || !currencyMatches) {
                log.error("[PaymentService] Idempotency key conflict for order: key={} already used with different parameters.", paymentReference);
                throw new IllegalStateException("Idempotency key '" + paymentReference + "' was already used with different request parameters.");
            }

            log.info("[PaymentService] Idempotent order hit for key={}, gatewayOrderId={}",
                    paymentReference, p.getGatewayOrderId());
            return CreatePaymentOrderResponse.builder()
                    .paymentReference(p.getIdempotencyKey())
                    .gatewayOrderId(p.getGatewayOrderId())
                    .amount(p.getAmount())
                    .currency(p.getCurrency())
                    .keyId(gatewayProvider.getKeyId())
                    .provider(p.getProvider())
                    .status(p.getStatus().name())
                    .build();
        }

        // Call Gateway Provider to create server-side order
        GatewayOrderResponse gatewayOrder = gatewayProvider.createOrder(
                paymentReference, subscription.getPrice(), currency
        );

        Payment payment = Payment.builder()
                .subscription(subscription)
                .user(subscription.getUser())
                .idempotencyKey(paymentReference)
                .gatewayOrderId(gatewayOrder.orderId())
                .amount(subscription.getPrice())
                .currency(currency)
                .provider(gatewayProvider.getProviderName())
                .status(PaymentStatus.PENDING)
                .attempts(0)
                .createdAt(LocalDate.ofInstant(now, ZoneOffset.UTC))
                .createdAtInstant(now)
                .autoRenew(false)
                .build();

        Payment saved;
        try {
            saved = paymentRepository.save(payment);
            log.info("[PaymentService] Payment order created: id={}, gatewayOrderId={}, ref={}",
                    saved.getId(), saved.getGatewayOrderId(), saved.getIdempotencyKey());
        } catch (DataIntegrityViolationException ex) {
            log.info("[PaymentService] Concurrent duplicate order detected for ref={}. Recovering existing record.", paymentReference);
            saved = paymentRepository.findByIdempotencyKey(paymentReference)
                    .orElseThrow(() -> ex);
        }

        return CreatePaymentOrderResponse.builder()
                .paymentReference(saved.getIdempotencyKey())
                .gatewayOrderId(saved.getGatewayOrderId())
                .amount(saved.getAmount())
                .currency(saved.getCurrency())
                .keyId(gatewayOrder.keyId())
                .provider(saved.getProvider())
                .status(saved.getStatus().name())
                .build();
    }

    /**
     * GATEWAY WEBHOOK VERIFICATION: Authenticates and idempotently processes provider webhooks.
     * Enforces HMAC-SHA256 signature verification, DB uniqueness, amount/currency matching,
     * and safe terminal state transitions.
     */
    @Transactional
    public Payment processGatewayWebhook(
            String rawPayload,
            String signatureHeader,
            String eventIdHeader
    ) {
        log.info("[PaymentService] Processing gateway webhook callback");

        // 1. Webhook Signature Verification (HMAC-SHA256 constant-time check)
        if (!gatewayProvider.verifyWebhookSignature(rawPayload, signatureHeader)) {
            log.error("[PaymentService] Webhook rejected: Invalid cryptographic signature header");
            throw new SecurityException("Invalid webhook signature header");
        }

        // 2. Deserialize webhook payload
        WebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawPayload, WebhookPayload.class);
        } catch (Exception e) {
            log.error("[PaymentService] Malformed webhook JSON payload: {}", e.getMessage());
            throw new IllegalArgumentException("Malformed webhook JSON payload", e);
        }

        String eventId = payload.resolveEventId(eventIdHeader);
        if (eventId == null || eventId.isBlank()) {
            eventId = "evt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        String orderId = payload.resolveOrderId();
        if (orderId == null || orderId.isBlank()) {
            log.error("[PaymentService] Webhook payload missing gateway orderId reference");
            throw new IllegalArgumentException("Webhook payload missing gateway order ID reference");
        }

        String eventType = payload.resolveEventType();
        Instant now = Instant.now();

        // 3. Database-backed Idempotency Check with isolated transaction boundary:
        // Uses REQUIRES_NEW in WebhookEventService so unique-constraint collisions on eventId
        // do not mark this outer transaction rollback-only or abort subsequent queries.
        boolean isNewClaim;
        try {
            isNewClaim = webhookEventService.recordEventIfNew(eventId, eventType, gatewayProvider.getProviderName());
        } catch (DataIntegrityViolationException | org.springframework.transaction.UnexpectedRollbackException ex) {
            log.info("[PaymentService] Concurrent duplicate webhook event {} detected (constraint collision). Sub-transaction rolled back cleanly.", eventId);
            isNewClaim = false;
        }

        if (!isNewClaim) {
            log.info("[PaymentService] Duplicate webhook event {} detected. Returning existing payment idempotently.", eventId);
            return paymentRepository.findByGatewayOrderId(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Payment not found for orderId: " + orderId));
        }

        // 4. Locate Payment and Subscription
        Payment payment = paymentRepository.findByGatewayOrderId(orderId)
                .orElseThrow(() -> {
                    log.error("[PaymentService] Unknown gateway reference: orderId={}", orderId);
                    return new IllegalArgumentException("Payment order not found for gateway order ID: " + orderId);
                });

        Subscription subscription = payment.getSubscription();
        if (subscription == null) {
            throw new IllegalStateException("Payment is not associated with any subscription");
        }

        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            log.warn("[PaymentService] Webhook rejected: Subscription id={} is CANCELLED", subscription.getId());
            throw new IllegalStateException("Cannot process webhook for a CANCELLED subscription.");
        }

        // 5. Amount & Currency Verification
        Double receivedAmount = payload.resolveAmount();
        String receivedCurrency = payload.resolveCurrency();

        String lowerEventType = eventType.toLowerCase(Locale.ROOT);
        boolean isSuccessEvent = lowerEventType.contains("captured") || lowerEventType.contains("authorized")
                || lowerEventType.contains("success") || lowerEventType.contains("paid");
        boolean isFailureEvent = lowerEventType.contains("failed") || lowerEventType.contains("error");

        if (isSuccessEvent) {
            if (receivedAmount == null || receivedAmount <= 0) {
                log.error("[PaymentService] Webhook payload missing payment amount or non-positive amount for orderId={}", orderId);
                String failureReason = "Webhook payload missing payment amount or non-positive amount";
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(failureReason);
                webhookEventService.recordDurablePaymentFailure(orderId, eventId, failureReason);
                throw new IllegalArgumentException(failureReason);
            }

            double expectedAmount = payment.getAmount() != null ? payment.getAmount() : subscription.getPrice();
            // Supports both normalized units (49.99) and minor currency units (paise/cents, e.g. 4999)
            boolean amountMatches = Math.abs(expectedAmount - receivedAmount) < 0.01
                    || Math.abs(expectedAmount * 100 - receivedAmount) < 0.01;

            if (!amountMatches) {
                log.error("[PaymentService] Payment amount mismatch: expected={}, received={}", expectedAmount, receivedAmount);
                String failureReason = "Amount mismatch: expected " + expectedAmount + ", received " + receivedAmount;
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(failureReason);
                webhookEventService.recordDurablePaymentFailure(orderId, eventId, failureReason);
                throw new IllegalArgumentException("Verified amount does not match expected subscription price");
            }
        }

        if (receivedCurrency != null && payment.getCurrency() != null) {
            if (!payment.getCurrency().equalsIgnoreCase(receivedCurrency)) {
                log.error("[PaymentService] Payment currency mismatch: expected={}, received={}", payment.getCurrency(), receivedCurrency);
                String failureReason = "Currency mismatch: expected " + payment.getCurrency() + ", received " + receivedCurrency;
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(failureReason);
                webhookEventService.recordDurablePaymentFailure(orderId, eventId, failureReason);
                throw new IllegalArgumentException("Currency does not match expected payment currency");
            }
        }

        // 6. Safe State Transitions
        PaymentStatus currentStatus = payment.getStatus();

        if (isSuccessEvent) {
            if (currentStatus == PaymentStatus.SUCCESS) {
                log.info("[PaymentService] Payment id={} is already SUCCESS. Duplicate webhook ignored, returning without re-extending.",
                        payment.getId());
                return payment;
            }

            if (currentStatus == PaymentStatus.CANCELLED) {
                throw new IllegalStateException("Unsupported state transition from CANCELLED to SUCCESS");
            }

            // Valid transition to SUCCESS: extend subscription duration exactly once
            extendSubscriptionDuration(subscription, now);

            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setProcessedAtInstant(now);
            if (payload.resolvePaymentId() != null) {
                payment.setGatewayPaymentId(payload.resolvePaymentId());
            }
            payment.setGatewaySignature(signatureHeader);
            payment.setFailureReason(null);
            payment.setNextRetryAt(null);
            payment.setNextRetryInstant(null);

            Payment saved = paymentRepository.save(payment);
            webhookEventService.updateEventStatus(eventId, "PROCESSED");
            log.info("[PaymentService] Verified webhook payment SUCCESS for orderId={}, new expiry={}",
                    orderId, subscription.getExpiresAt());
            return saved;
        } else if (isFailureEvent) {
            if (currentStatus == PaymentStatus.SUCCESS) {
                // Out-of-order or delayed failure arriving after verified SUCCESS must be safely ignored
                log.warn("[PaymentService] Out-of-order/delayed failure webhook received for already SUCCESS payment id={}, ignoring.",
                        payment.getId());
                return payment;
            }

            // Failed payment: status to FAILED, entitlement is strictly NOT granted
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(payload.resolveFailureReason());
            payment.setProcessedAtInstant(now);
            payment.setNextRetryAt(null);
            payment.setNextRetryInstant(null);

            Payment saved = paymentRepository.save(payment);
            webhookEventService.updateEventStatus(eventId, "FAILED");
            log.warn("[PaymentService] Payment orderId={} marked FAILED from gateway webhook: {}",
                    orderId, payment.getFailureReason());
            return saved;
        } else {
            log.info("[PaymentService] Unhandled webhook event type: {}", eventType);
            return payment;
        }
    }

    /**
     * USER: Get my payments
     */
    public List<Payment> getPaymentsByUser(String email) {
        return paymentRepository.findBySubscriptionUserEmail(email);
    }

    /**
     * ADMIN: Get all payments
     */
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }
}
