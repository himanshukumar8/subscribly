package com.subscribly.subscriptionservice.controller;

import com.subscribly.subscriptionservice.entity.Subscription;
import com.subscribly.subscriptionservice.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/subscriptions")
@RequiredArgsConstructor
public class AdminSubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/all")
    public List<Subscription> getAllSubscriptions() {
        return subscriptionService.getAllSubscriptions();
    }

    @PutMapping("/{id}/status")
    public Subscription updateStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        return subscriptionService.updateStatus(id, status);
    }
}
