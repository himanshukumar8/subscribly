package com.subscribly.subscriptionservice.service;

import com.subscribly.common.dto.UserDto;
import com.subscribly.common.event.SubscriptionCreatedEvent;
import com.subscribly.common.event.SubscriptionExpiredEvent;
import com.subscribly.common.event.SubscriptionRenewedEvent;
import com.subscribly.subscriptionservice.client.UserClient;
import com.subscribly.subscriptionservice.dto.SubscriptionRequest;
import com.subscribly.subscriptionservice.entity.BillingCycle;
import com.subscribly.subscriptionservice.entity.Subscription;
import com.subscribly.subscriptionservice.entity.SubscriptionStatus;
import com.subscribly.subscriptionservice.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserClient userClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public SubscriptionService(
            SubscriptionRepository subscriptionRepository,
            UserClient userClient,
            KafkaTemplate<String, Object> kafkaTemplate
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.userClient = userClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    private static final String TOPIC = "subscription-events";

    @Transactional
    public Subscription createSubscription(String email, SubscriptionRequest req) {
        log.info("[SubscriptionService] Creating subscription for user email: {}", email);
        UserDto userDto = userClient.getUserByEmail(email);
        if (userDto == null) {
            throw new RuntimeException("User not found: " + email);
        }

        Instant now = Instant.now();
        BillingCycle cycle = req.getBillingCycle() != null ? req.getBillingCycle() : BillingCycle.MONTHLY;
        Instant expiresAt = now.atZone(ZoneOffset.UTC).plusMonths(cycle.getMonths()).toInstant();

        Subscription subscription = Subscription.builder()
                .userId(userDto.getId())
                .userEmail(userDto.getEmail())
                .plan(req.getPlan())
                .price(req.getPrice())
                .billingCycle(cycle)
                .status(SubscriptionStatus.ACTIVE)
                .startedAt(now)
                .expiresAt(expiresAt)
                .startDate(LocalDate.ofInstant(now, ZoneOffset.UTC))
                .endDate(LocalDate.ofInstant(expiresAt, ZoneOffset.UTC))
                .autoRenew(true)
                .build();

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("[SubscriptionService] Subscription saved with ID: {}, expiresAt: {}", saved.getId(), saved.getExpiresAt());

        // Publish event
        SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(
                saved.getId(),
                saved.getUserId(),
                saved.getPlan(),
                saved.getBillingCycle().name(),
                saved.getPrice()
        );
        publishEventSafely(String.valueOf(saved.getId()), event);

        return saved;
    }

    @Transactional
    public List<Subscription> getSubscriptionsByUser(String email) {
        List<Subscription> list = subscriptionRepository.findByUserEmail(email);
        Instant now = Instant.now();
        for (Subscription s : list) {
            reconcileIfExpired(s, now);
        }
        return list;
    }

    @Transactional
    public List<Subscription> getAllSubscriptions() {
        List<Subscription> list = subscriptionRepository.findAll();
        Instant now = Instant.now();
        for (Subscription s : list) {
            reconcileIfExpired(s, now);
        }
        return list;
    }

    @Transactional
    public Subscription validateSubscription(Long id) {
        Subscription s = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + id));
        reconcileIfExpired(s, Instant.now());
        return s;
    }

    @Transactional
    public Subscription updateStatus(Long id, String status) {
        Subscription s = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        s.setStatus(SubscriptionStatus.valueOf(status.toUpperCase()));
        Subscription updated = subscriptionRepository.save(s);

        if ("EXPIRED".equalsIgnoreCase(status)) {
            SubscriptionExpiredEvent event = new SubscriptionExpiredEvent(
                    updated.getId(),
                    updated.getUserId(),
                    updated.getPlan()
            );
            publishEventSafely(String.valueOf(updated.getId()), event);
        }

        return updated;
    }

    @Transactional
    public Subscription cancelSubscription(Long id, String email) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (!subscription.getUserEmail().equals(email)) {
            throw new RuntimeException("Unauthorized: You cannot cancel this subscription");
        }

        Instant now = Instant.now();
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setExpiresAt(now);
        subscription.setEndDate(LocalDate.ofInstant(now, ZoneOffset.UTC));
        subscription.setAutoRenew(false);

        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public Subscription updateBillingCycle(Long id, String email, BillingCycle newCycle) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (!subscription.getUserEmail().equals(email)) {
            throw new RuntimeException("Unauthorized: You cannot update this subscription");
        }

        Instant now = Instant.now();
        subscription.setBillingCycle(newCycle);

        Instant baseTime = subscription.getEffectiveExpiryInstant() != null && subscription.getEffectiveExpiryInstant().isAfter(now)
                ? subscription.getEffectiveExpiryInstant()
                : now;
        Instant newExpiry = baseTime.atZone(ZoneOffset.UTC).plusMonths(newCycle.getMonths()).toInstant();

        subscription.setExpiresAt(newExpiry);
        subscription.setEndDate(LocalDate.ofInstant(newExpiry, ZoneOffset.UTC));
        Subscription updated = subscriptionRepository.save(subscription);

        SubscriptionRenewedEvent event = new SubscriptionRenewedEvent(
                updated.getId(),
                updated.getUserId(),
                updated.getPlan(),
                updated.getEndDate().toString()
        );
        publishEventSafely(String.valueOf(updated.getId()), event);

        return updated;
    }

    private void reconcileIfExpired(Subscription s, Instant now) {
        if (s.getStatus() == SubscriptionStatus.ACTIVE && s.isExpired(now)) {
            log.info("[SubscriptionService] Lazy reconciliation: subscription id={} expired at {}. Transitioning to EXPIRED.",
                    s.getId(), s.getEffectiveExpiryInstant());
            s.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(s);

            SubscriptionExpiredEvent event = new SubscriptionExpiredEvent(
                    s.getId(),
                    s.getUserId(),
                    s.getPlan()
            );
            publishEventSafely(String.valueOf(s.getId()), event);
        }
    }

    private void publishEventSafely(String key, Object event) {
        try {
            kafkaTemplate.send(TOPIC, key, event);
            log.info("[SubscriptionService] Published event to topic: {}, key: {}", TOPIC, key);
        } catch (Exception e) {
            log.warn("[SubscriptionService] Could not publish event to Kafka (broker unreachable or timeout): {}", e.getMessage());
        }
    }
}

