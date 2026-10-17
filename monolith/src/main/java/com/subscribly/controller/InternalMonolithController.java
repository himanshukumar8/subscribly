package com.subscribly.controller;

import com.subscribly.repository.SubscriptionRepository;
import com.subscribly.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
public class InternalMonolithController {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;

    @GetMapping("/subscriptions/{id}")
    public ResponseEntity<?> getInternalSubscription(@PathVariable Long id) {
        log.info("[InternalAPI] Service-to-service lookup for Subscription ID: {}", id);
        return subscriptionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/payments/{id}")
    public ResponseEntity<?> getInternalPayment(@PathVariable Long id) {
        log.info("[InternalAPI] Service-to-service lookup for Payment ID: {}", id);
        return paymentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
