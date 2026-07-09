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

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {

    private final Logger log = LoggerFactory.getLogger(SubscriptionScheduler.class);

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final PaymentRepository paymentRepository;

   
    @Scheduled(fixedRate = 300000)
    public void processContinuousAutoRenewals() {

        log.info("SubscriptionScheduler: Micro-batch auto-renew job started");

       
        LocalDateTime now = LocalDateTime.now();

        
        Pageable limit = PageRequest.of(0, 500);

     
        List<Subscription> dueSubscriptions = subscriptionRepository
                .findDueSubscriptions(SubscriptionStatus.ACTIVE, now, limit);

        if (!dueSubscriptions.isEmpty()) {
            log.info("Found {} active subscriptions due for renewal.", dueSubscriptions.size());
            for (Subscription s : dueSubscriptions) {
                log.info("Creating auto-renew payment for subscription id {}", s.getId());
                subscriptionService.createAutoRenewPaymentForSubscription(s);
            }
        }

      
        List<Payment> paymentsToProcess = paymentRepository
                .findPaymentsToProcess(PaymentStatus.RETRYING, PaymentStatus.PENDING, now, limit);

        if (!paymentsToProcess.isEmpty()) {
            log.info("Found {} payments to process.", paymentsToProcess.size());
            for (Payment p : paymentsToProcess) {
                log.info("Processing payment id={} attempts={}", p.getId(), p.getAttempts());
                boolean success = subscriptionService.processPayment(p);
                log.info("Payment id={} processed -> success={}", p.getId(), success);
            }
        }

        log.info("SubscriptionScheduler: Micro-batch job finished");
    }
}
