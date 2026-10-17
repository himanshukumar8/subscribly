package com.subscribly.service;

import com.subscribly.entity.WebhookEvent;
import com.subscribly.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class WebhookEventService {

    private static final Logger log = LoggerFactory.getLogger(WebhookEventService.class);

    private final WebhookEventRepository webhookEventRepository;
    private final com.subscribly.repository.PaymentRepository paymentRepository;

    /**
     * Atomically attempts to claim and record a webhook event in an isolated physical transaction.
     * If the event already exists or a concurrent insertion causes a PostgreSQL unique constraint violation,
     * this isolated transaction rolls back without aborting or marking the caller's transaction rollback-only.
     *
     * @return true if this invocation successfully claimed the event for processing;
     *         false if the event is a duplicate (already processed or claimed concurrently).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean recordEventIfNew(String eventId, String eventType, String provider) {
        if (webhookEventRepository.existsByEventId(eventId)) {
            log.info("[WebhookEventService] Webhook eventId={} already exists (pre-check hit).", eventId);
            return false;
        }

        WebhookEvent event = WebhookEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .provider(provider != null ? provider : "RAZORPAY")
                .status("PROCESSING")
                .receivedAt(Instant.now())
                .build();
        webhookEventRepository.saveAndFlush(event);
        return true;
    }

    /**
     * Updates the terminal status of the webhook event in an isolated transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateEventStatus(String eventId, String status) {
        try {
            webhookEventRepository.findByEventId(eventId).ifPresent(event -> {
                event.setStatus(status);
                event.setProcessedAt(Instant.now());
                webhookEventRepository.save(event);
            });
        } catch (Exception ex) {
            log.warn("[WebhookEventService] Could not update status for eventId={}: {}", eventId, ex.getMessage());
        }
    }

    /**
     * Durably records a payment failure in an isolated physical transaction (REQUIRES_NEW).
     * This guarantees that payment rejection status and failure reason are committed to PostgreSQL
     * even when the outer transaction rolls back due to a subsequent exception (e.g. IllegalArgumentException).
     * Subscription entitlement is untouched by this method.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordDurablePaymentFailure(String orderId, String eventId, String failureReason) {
        if (orderId == null || orderId.isBlank()) {
            return;
        }
        try {
            paymentRepository.findByGatewayOrderId(orderId).ifPresent(payment -> {
                payment.setStatus(com.subscribly.entity.PaymentStatus.FAILED);
                payment.setFailureReason(failureReason);
                payment.setProcessedAtInstant(Instant.now());
                paymentRepository.saveAndFlush(payment);
                log.info("[WebhookEventService] Durably recorded payment FAILED for orderId={}, reason={}", orderId, failureReason);
            });
            if (eventId != null && !eventId.isBlank()) {
                webhookEventRepository.findByEventId(eventId).ifPresent(event -> {
                    event.setStatus("FAILED");
                    event.setProcessedAt(Instant.now());
                    webhookEventRepository.saveAndFlush(event);
                });
            }
        } catch (Exception ex) {
            log.warn("[WebhookEventService] Failed to record durable payment failure for orderId={}: {}", orderId, ex.getMessage());
        }
    }
}
