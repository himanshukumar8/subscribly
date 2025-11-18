package com.subscribly.service;

import com.subscribly.entity.Payment;
import com.subscribly.entity.PaymentStatus;
import com.subscribly.entity.Subscription;
import com.subscribly.repository.PaymentRepository;
import com.subscribly.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;

    /**
     * USER: Make manual payment
     */
    public Payment makePayment(Long subscriptionId, String email) {

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (!subscription.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized payment attempt");
        }

        Payment payment = Payment.builder()
                .subscription(subscription)
                .amount(subscription.getPrice())
                .status(PaymentStatus.SUCCESS)
                .attempts(1)
                .nextRetryAt(null)
                .createdAt(LocalDate.now())
                .autoRenew(false)
                .build();

        // extend subscription duration based on billing cycle
        subscription.setEndDate(LocalDate.now()
                .plusMonths(subscription.getBillingCycle().getMonths()));

        subscriptionRepository.save(subscription);

        return paymentRepository.save(payment);
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
