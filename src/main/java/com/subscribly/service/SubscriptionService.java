package com.subscribly.service;

import com.subscribly.dto.SubscriptionRequest;
import com.subscribly.entity.*;
import com.subscribly.repository.SubscriptionRepository;
import com.subscribly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    /* Create new subscription */
    public Subscription createSubscription(String email, SubscriptionRequest req) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(req.getPlan())
                .price(req.getPrice())
                .billingCycle(req.getBillingCycle())
                .status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(req.getBillingCycle().getMonths()))
                .build();

        return subscriptionRepository.save(subscription);
    }

    /* Fetch logged-in user's subscriptions */
    public List<Subscription> getSubscriptionsByUser(String email) {
        return subscriptionRepository.findByUserEmail(email);
    }

    /* ADMIN: get all subscriptions */
    public List<Subscription> getAllSubscriptions() {
        return subscriptionRepository.findAll();
    }

    /* ADMIN: Update subscription status (ACTIVE / CANCELED / EXPIRED) */
    public Subscription updateStatus(Long id, String status) {
        Subscription s = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        s.setStatus(SubscriptionStatus.valueOf(status.toUpperCase()));

        return subscriptionRepository.save(s);
    }

    /* USER: Cancel their subscription */
    public Subscription cancelSubscription(Long id, String email) {

        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (!subscription.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized: You cannot cancel this subscription");
        }

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setEndDate(LocalDate.now());

        return subscriptionRepository.save(subscription);
    }


    public Subscription updateBillingCycle(Long id, String email, BillingCycle newCycle) {

        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (!subscription.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized: You cannot update this subscription");
        }

        subscription.setBillingCycle(newCycle);
        subscription.setEndDate(LocalDate.now().plusMonths(newCycle.getMonths()));

        return subscriptionRepository.save(subscription);
    }


    /* Auto-renew helper for scheduler (placeholder for now) */
    public void createAutoRenewPaymentForSubscription(Subscription subscription) {
        // we will implement this after payment logic
        // leave empty to avoid errors
    }

    public boolean processPayment(Payment payment) {
        // payment processing implemented in PaymentService
        return true;
    }

}
