package com.subscribly.service;

import com.subscribly.dto.SubscriptionRequest;
import com.subscribly.dto.SubscriptionResponse;
import com.subscribly.dto.SubscriptionValidationDto;
import com.subscribly.entity.*;
import com.subscribly.repository.PaymentRepository;
import com.subscribly.repository.SubscriptionRepository;
import com.subscribly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;

    /**
     * Create new subscription with exact startedAt and expiresAt timestamps.
     */
    @Transactional
    public SubscriptionResponse createSubscription(String email, SubscriptionRequest req) {
        log.info("[SubscriptionService] Creating subscription for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        Instant now = Instant.now();
        BillingCycle cycle = req.getBillingCycle() != null ? req.getBillingCycle() : BillingCycle.MONTHLY;
        Instant expiresAt = now.atZone(ZoneOffset.UTC).plusMonths(cycle.getMonths()).toInstant();

        String plan = (req.getPlan() != null && !req.getPlan().isBlank()) ? req.getPlan().trim() : "PRO";
        Double price = (req.getPrice() != null && req.getPrice() >= 0) ? req.getPrice() : 49.99;

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .price(price)
                .billingCycle(cycle)
                .status(SubscriptionStatus.ACTIVE)
                .startedAt(now)
                .expiresAt(expiresAt)
                .startDate(LocalDate.ofInstant(now, ZoneOffset.UTC))
                .endDate(LocalDate.ofInstant(expiresAt, ZoneOffset.UTC))
                .autoRenew(req.getAutoRenew() != null ? req.getAutoRenew() : true)
                .build();

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("[SubscriptionService] Subscription created id={}, expiresAt={}", saved.getId(), saved.getExpiresAt());
        return mapToResponse(saved, now);
    }

    /**
     * Fetch user's subscriptions with real-time expiration evaluation & lazy state reconciliation.
     * Does NOT depend on midnight batch jobs.
     */
    @Transactional
    public List<SubscriptionResponse> getSubscriptionsByUser(String email) {
        List<Subscription> subscriptions = subscriptionRepository.findByUserEmail(email);
        Instant now = Instant.now();

        for (Subscription s : subscriptions) {
            reconcileIfExpired(s, now);
        }

        return subscriptions.stream()
                .map(s -> mapToResponse(s, now))
                .toList();
    }

    /**
     * ADMIN: get all subscriptions with real-time status.
     */
    @Transactional
    public List<SubscriptionResponse> getAllSubscriptions() {
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        Instant now = Instant.now();

        for (Subscription s : subscriptions) {
            reconcileIfExpired(s, now);
        }

        return subscriptions.stream()
                .map(s -> mapToResponse(s, now))
                .toList();
    }

    /**
     * Real-time entitlement validation for API Gateway or downstream service access checks.
     * Evaluates exact timestamp as the single source of truth.
     */
    @Transactional
    public SubscriptionValidationDto validateSubscription(Long id) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found with ID: " + id));

        Instant now = Instant.now();
        reconcileIfExpired(subscription, now);

        boolean active = subscription.hasActiveEntitlement(now);
        SubscriptionStatus effectiveStatus = subscription.getEffectiveStatus(now);

        long remainingSeconds = 0;
        Instant expiry = subscription.getEffectiveExpiryInstant();
        if (expiry != null && expiry.isAfter(now)) {
            remainingSeconds = Duration.between(now, expiry).getSeconds();
        }

        return SubscriptionValidationDto.builder()
                .subscriptionId(subscription.getId())
                .userEmail(subscription.getUser() != null ? subscription.getUser().getEmail() : null)
                .plan(subscription.getPlan())
                .price(subscription.getPrice())
                .billingCycle(subscription.getBillingCycle())
                .persistedStatus(subscription.getStatus())
                .effectiveStatus(effectiveStatus)
                .active(active)
                .startedAt(subscription.getStartedAt())
                .expiresAt(expiry)
                .remainingSeconds(remainingSeconds)
                .autoRenew(subscription.isAutoRenew())
                .build();
    }

    /**
     * ADMIN: Update subscription status (ACTIVE / CANCELLED / EXPIRED).
     */
    @Transactional
    public SubscriptionResponse updateStatus(Long id, String status) {
        Subscription s = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        SubscriptionStatus newStatus = SubscriptionStatus.valueOf(status.toUpperCase());
        s.setStatus(newStatus);
        Subscription updated = subscriptionRepository.save(s);
        return mapToResponse(updated, Instant.now());
    }

    /**
     * USER: Cancel their subscription immediately.
     */
    @Transactional
    public SubscriptionResponse cancelSubscription(Long id, String email) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (!subscription.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized: You cannot cancel this subscription");
        }

        Instant now = Instant.now();
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setExpiresAt(now);
        subscription.setEndDate(LocalDate.ofInstant(now, ZoneOffset.UTC));
        subscription.setAutoRenew(false);

        Subscription updated = subscriptionRepository.save(subscription);
        log.info("[SubscriptionService] Subscription id={} cancelled by user {}", id, email);
        return mapToResponse(updated, now);
    }

    /**
     * USER: Update billing cycle. Recalculates exact expiry boundary.
     */
    @Transactional
    public SubscriptionResponse updateBillingCycle(Long id, String email, BillingCycle newCycle) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (!subscription.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized: You cannot update this subscription");
        }

        Instant now = Instant.now();
        subscription.setBillingCycle(newCycle);

        // Calculate new expiry from now or current valid expiry
        Instant baseTime = subscription.getEffectiveExpiryInstant() != null && subscription.getEffectiveExpiryInstant().isAfter(now)
                ? subscription.getEffectiveExpiryInstant()
                : now;
        Instant newExpiry = baseTime.atZone(ZoneOffset.UTC).plusMonths(newCycle.getMonths()).toInstant();

        subscription.setExpiresAt(newExpiry);
        subscription.setEndDate(LocalDate.ofInstant(newExpiry, ZoneOffset.UTC));

        Subscription updated = subscriptionRepository.save(subscription);
        log.info("[SubscriptionService] Subscription id={} cycle updated to {}, new expiry: {}", id, newCycle, newExpiry);
        return mapToResponse(updated, now);
    }

    /**
     * Auto-renew processing executed by continuous scheduler or proactive sweep.
     * Evaluates exact expiry timestamp and extends cycle if autoRenew is enabled.
     */
    @Transactional
    public Payment createAutoRenewPaymentForSubscription(Subscription subscription) {
        Instant now = Instant.now();

        // If subscription is cancelled, retain CANCELLED status and do not auto-renew
        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            log.info("[SubscriptionService] Subscription id={} is cancelled. Skipping auto-renew.", subscription.getId());
            return null;
        }

        // If auto-renew is disabled, transition to EXPIRED
        if (!subscription.isAutoRenew()) {
            log.info("[SubscriptionService] Subscription id={} has autoRenew=false. Marking EXPIRED.", subscription.getId());
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(subscription);
            return null;
        }

        // Generate deterministic idempotency key for this renewal period
        String renewalKey = "renew-sub-" + subscription.getId() + "-" +
                (subscription.getEffectiveExpiryInstant() != null ? subscription.getEffectiveExpiryInstant().getEpochSecond() : "init");

        // Idempotency check: if this renewal cycle was already processed, return existing payment without double extending
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(renewalKey);
        if (existing.isPresent()) {
            log.info("[SubscriptionService] Renewal attempt with key={} already processed. Skipping duplicate extension.", renewalKey);
            return existing.get();
        }

        // Advance exact expiry timestamp from the previous expiry or now
        Instant baseTime = subscription.getEffectiveExpiryInstant() != null && subscription.getEffectiveExpiryInstant().isAfter(now)
                ? subscription.getEffectiveExpiryInstant()
                : now;
        Instant newExpiry = baseTime.atZone(ZoneOffset.UTC)
                .plusMonths(subscription.getBillingCycle().getMonths())
                .toInstant();

        subscription.setExpiresAt(newExpiry);
        subscription.setEndDate(LocalDate.ofInstant(newExpiry, ZoneOffset.UTC));
        subscription.setStatus(SubscriptionStatus.ACTIVE);

        // Record successful renewal payment with idempotency reference
        Payment payment = Payment.builder()
                .subscription(subscription)
                .user(subscription.getUser())
                .idempotencyKey(renewalKey)
                .amount(subscription.getPrice())
                .status(PaymentStatus.SUCCESS)
                .attempts(1)
                .nextRetryAt(null)
                .createdAt(LocalDate.ofInstant(now, ZoneOffset.UTC))
                .createdAtInstant(now)
                .processedAtInstant(now)
                .autoRenew(true)
                .build();

        try {
            subscriptionRepository.save(subscription);
            Payment savedPayment = paymentRepository.save(payment);
            log.info("[SubscriptionService] Auto-renew succeeded for subscription id={}, new expiresAt={}, paymentId={}, idempotencyKey={}",
                    subscription.getId(), newExpiry, savedPayment.getId(), renewalKey);
            return savedPayment;
        } catch (org.springframework.dao.DataIntegrityViolationException | org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
            log.warn("[SubscriptionService] Concurrent renewal collision detected on key={}. Returning winning payment.", renewalKey);
            return paymentRepository.findByIdempotencyKey(renewalKey).orElse(null);
        }
    }

    /**
     * Payment processor helper.
     */
    public boolean processPayment(Payment payment) {
        return payment != null && payment.getStatus() == PaymentStatus.SUCCESS;
    }

    /**
     * Lazy reconciliation: If the DB currently says ACTIVE, but the exact timestamp has passed,
     * transition DB record to EXPIRED immediately without waiting for a midnight batch job.
     */
    private void reconcileIfExpired(Subscription s, Instant now) {
        if (s.getStatus() == SubscriptionStatus.ACTIVE && s.isExpired(now)) {
            log.info("[SubscriptionService] Lazy reconciliation: subscription id={} expired at {}. Updating status to EXPIRED.",
                    s.getId(), s.getEffectiveExpiryInstant());
            s.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(s);
        }
    }

    /**
     * Helper to map Subscription entity to SubscriptionResponse DTO.
     */
    public SubscriptionResponse mapToResponse(Subscription s, Instant now) {
        Instant expiry = s.getEffectiveExpiryInstant();
        boolean isExpired = s.isExpired(now);
        SubscriptionStatus effectiveStatus = s.getEffectiveStatus(now);

        long remainingSeconds = 0;
        if (expiry != null && expiry.isAfter(now)) {
            remainingSeconds = Duration.between(now, expiry).getSeconds();
        }

        return SubscriptionResponse.builder()
                .id(s.getId())
                .plan(s.getPlan())
                .serviceName(s.getPlan())
                .price(s.getPrice())
                .billingCycle(s.getBillingCycle())
                .status(s.getStatus())
                .effectiveStatus(effectiveStatus)
                .active(effectiveStatus == SubscriptionStatus.ACTIVE)
                .startedAt(s.getStartedAt())
                .expiresAt(expiry)
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .nextBillingDate(s.getEndDate())
                .autoRenew(s.isAutoRenew())
                .remainingSeconds(remainingSeconds)
                .build();
    }
}

