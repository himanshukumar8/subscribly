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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {

    private final Logger log = LoggerFactory.getLogger(SubscriptionScheduler.class);

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final PaymentRepository paymentRepository;

    @Scheduled(cron = "0 0 0 * * *")
    public void nightlyAutoRenewJob() {

        log.info("SubscriptionScheduler: nightlyAutoRenewJob started");

        LocalDate today = LocalDate.now();

        List<Subscription> dueSubscriptions = subscriptionRepository.findAll().stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .filter(s -> s.getEndDate() != null && !s.getEndDate().isAfter(today))
                .toList();

        for (Subscription s : dueSubscriptions) {
            log.info("Creating auto-renew payment for subscription id {}", s.getId());
            subscriptionService.createAutoRenewPaymentForSubscription(s);
        }

        List<Payment> retryPayments = paymentRepository.findByStatusAndNextRetryAtBefore(
                PaymentStatus.RETRYING, today.plusDays(1));

        List<Payment> pendingPayments = paymentRepository.findByStatus(PaymentStatus.PENDING);

        retryPayments.addAll(pendingPayments);

        for (Payment p : retryPayments) {
            log.info("Processing payment id={} attempts={}", p.getId(), p.getAttempts());
            boolean success = subscriptionService.processPayment(p);
            log.info("Payment id={} processed -> success={}", p.getId(), success);
        }

        log.info("SubscriptionScheduler: nightlyAutoRenewJob finished");
    }
}
