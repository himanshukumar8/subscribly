package com.subscribly.scheduler;

import com.subscribly.entity.Payment;
import com.subscribly.entity.PaymentStatus;
import com.subscribly.entity.Subscription;
import com.subscribly.entity.SubscriptionStatus;
import com.subscribly.repository.PaymentRepository;
import com.subscribly.repository.SubscriptionRepository;
import com.subscribly.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {

    private final Logger log = LoggerFactory.getLogger(SubscriptionScheduler.class);

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final PaymentRepository paymentRepository;
    private final com.subscribly.service.PaymentService paymentService;

    @Scheduled(fixedRate = 300000)
    public void processContinuousAutoRenewals() {

        log.info("SubscriptionScheduler: Micro-batch auto-renew job started");

        Instant now = Instant.now();
        java.time.LocalDate today = java.time.LocalDate.now();

        Pageable limit = PageRequest.of(0, 500);

        List<Subscription> dueSubscriptions = subscriptionRepository
                .findDueSubscriptions(SubscriptionStatus.ACTIVE, now, today, limit);

        if (!dueSubscriptions.isEmpty()) {
            log.info("Found {} active subscriptions due for renewal.", dueSubscriptions.size());
            for (Subscription s : dueSubscriptions) {
                log.info("Processing due subscription id {}", s.getId());
                subscriptionService.createAutoRenewPaymentForSubscription(s);
            }
        }

        List<Payment> paymentsToProcess = paymentRepository
                .findPaymentsToProcessInstant(PaymentStatus.RETRYING, PaymentStatus.PENDING, now, today, limit);

        if (!paymentsToProcess.isEmpty()) {
            log.info("Found {} payments to process.", paymentsToProcess.size());
            for (Payment p : paymentsToProcess) {
                log.info("Processing retry for payment id={} attempt={}", p.getId(), p.getAttempts());
                Payment retried = paymentService.retryPayment(p, now, true, null, com.subscribly.service.PaymentService.DEFAULT_BASE_BACKOFF);
                log.info("Payment id={} retried -> newStatus={}", retried.getId(), retried.getStatus());
            }
        }

        log.info("SubscriptionScheduler: Micro-batch job finished");
    }
}

