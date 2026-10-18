package com.subscribly.controller;

import com.subscribly.dto.PaymentResponse;
import com.subscribly.dto.SubscriptionResponse;
import com.subscribly.repository.PaymentRepository;
import com.subscribly.repository.SubscriptionRepository;
import com.subscribly.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@Slf4j
@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
public class InternalMonolithController {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionService subscriptionService;

    @GetMapping("/subscriptions/{id}")
    public ResponseEntity<SubscriptionResponse> getInternalSubscription(@PathVariable Long id) {
        log.info("[InternalAPI] Service-to-service lookup for Subscription ID: {}", id);
        return subscriptionRepository.findById(id)
                .map(s -> ResponseEntity.ok(subscriptionService.mapToResponse(s, Instant.now())))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/payments/{id}")
    public ResponseEntity<PaymentResponse> getInternalPayment(@PathVariable Long id) {
        log.info("[InternalAPI] Service-to-service lookup for Payment ID: {}", id);
        return paymentRepository.findById(id)
                .map(p -> ResponseEntity.ok(PaymentResponse.fromEntity(p)))
                .orElse(ResponseEntity.notFound().build());
    }
}
