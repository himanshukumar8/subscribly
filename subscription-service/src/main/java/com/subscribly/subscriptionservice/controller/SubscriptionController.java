package com.subscribly.subscriptionservice.controller;

import com.subscribly.subscriptionservice.dto.SubscriptionRequest;
import com.subscribly.subscriptionservice.entity.BillingCycle;
import com.subscribly.subscriptionservice.entity.Subscription;
import com.subscribly.subscriptionservice.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping({ "", "/create" })
    public Subscription createSubscription(
            @AuthenticationPrincipal String username,
            @Valid @RequestBody SubscriptionRequest req
    ) {
        String authUser = requireAuthenticatedUser(username);
        return subscriptionService.createSubscription(authUser, req);
    }

    @GetMapping("/my")
    public List<Subscription> getMySubscriptions(
            @AuthenticationPrincipal String username
    ) {
        String authUser = requireAuthenticatedUser(username);
        return subscriptionService.getSubscriptionsByUser(authUser);
    }

    @GetMapping("/{id}/validate")
    public Subscription validateSubscription(@PathVariable Long id) {
        return subscriptionService.validateSubscription(id);
    }

    @PutMapping("/{id}/status")
    public Subscription updateStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        return subscriptionService.updateStatus(id, status);
    }

    @PutMapping("/{id}/cancel")
    public Subscription cancelSubscription(
            @PathVariable Long id,
            @AuthenticationPrincipal String username
    ) {
        String authUser = requireAuthenticatedUser(username);
        return subscriptionService.cancelSubscription(id, authUser);
    }

    @PutMapping("/{id}/billing-cycle")
    public Subscription updateBillingCycle(
            @PathVariable Long id,
            @RequestParam String cycle,
            @AuthenticationPrincipal String username
    ) {
        String authUser = requireAuthenticatedUser(username);
        BillingCycle newCycle = BillingCycle.valueOf(cycle.toUpperCase());
        return subscriptionService.updateBillingCycle(id, authUser, newCycle);
    }

    private String requireAuthenticatedUser(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User authentication required");
        }
        return username;
    }
}
