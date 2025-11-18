package com.subscribly.controller;

import com.subscribly.dto.SubscriptionRequest;
import com.subscribly.entity.Subscription;
import com.subscribly.entity.BillingCycle;
import com.subscribly.service.SubscriptionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscription")
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    @PostMapping("/create")
    public Subscription createSubscription(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody SubscriptionRequest req
    ) {
        return subscriptionService.createSubscription(user.getUsername(), req);
    }

    @GetMapping("/my")
    public List<Subscription> getMySubscriptions(
            @AuthenticationPrincipal UserDetails user
    ) {
        return subscriptionService.getSubscriptionsByUser(user.getUsername());
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
            @AuthenticationPrincipal UserDetails user
    ) {
        return subscriptionService.cancelSubscription(id, user.getUsername());
    }

    @PutMapping("/{id}/billing-cycle")
    public Subscription updateBillingCycle(
            @PathVariable Long id,
            @RequestParam String cycle,
            @AuthenticationPrincipal UserDetails user
    ) {
        BillingCycle newCycle = BillingCycle.valueOf(cycle.toUpperCase());
        return subscriptionService.updateBillingCycle(id, user.getUsername(), newCycle);
    }

}
