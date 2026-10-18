package com.subscribly.subscriptionservice.controller;

import com.subscribly.common.dto.SubscriptionDto;
import com.subscribly.subscriptionservice.entity.Subscription;
import com.subscribly.subscriptionservice.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/internal/subscriptions")
@RequiredArgsConstructor
public class InternalSubscriptionController {

    private final SubscriptionRepository subscriptionRepository;

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionDto> getSubscriptionById(@PathVariable Long id) {
        log.info("[InternalAPI] Service-to-service lookup for Subscription ID: {}", id);
        return subscriptionRepository.findById(id)
                .map(sub -> ResponseEntity.ok(toDto(sub)))
                .orElse(ResponseEntity.notFound().build());
    }

    private SubscriptionDto toDto(Subscription sub) {
        return SubscriptionDto.builder()
                .id(sub.getId())
                .userId(sub.getUserId())
                .userEmail(sub.getUserEmail())
                .plan(sub.getPlan())
                .price(sub.getPrice())
                .billingCycle(sub.getBillingCycle().name())
                .status(sub.getStatus().name())
                .startDate(sub.getStartDate() != null ? sub.getStartDate().toString() : null)
                .endDate(sub.getEndDate() != null ? sub.getEndDate().toString() : null)
                .build();
    }
}
