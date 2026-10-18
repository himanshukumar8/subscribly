package com.subscribly.controller;

import com.subscribly.dto.SubscriptionRequest;
import com.subscribly.dto.SubscriptionResponse;
import com.subscribly.dto.SubscriptionValidationDto;
import com.subscribly.entity.BillingCycle;
import com.subscribly.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/create")
    public SubscriptionResponse createSubscription(
            Principal principal,
            @RequestBody SubscriptionRequest req
    ) {
        String authenticatedEmail = requireAuthenticatedEmail(principal);
        if (req != null && req.getEmail() != null && !req.getEmail().isBlank()
                && !req.getEmail().equalsIgnoreCase(authenticatedEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot create subscription for another user");
        }
        return subscriptionService.createSubscription(authenticatedEmail, req);
    }

    @GetMapping("/my")
    public List<SubscriptionResponse> getMySubscriptions(
            Principal principal,
            @RequestParam(required = false) String email
    ) {
        String authenticatedEmail = requireAuthenticatedEmail(principal);
        if (email != null && !email.isBlank() && !email.equalsIgnoreCase(authenticatedEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot access another user's subscriptions");
        }
        return subscriptionService.getSubscriptionsByUser(authenticatedEmail);
    }

    @GetMapping("/all")
    public List<SubscriptionResponse> getAllSubscriptions() {
        return subscriptionService.getAllSubscriptions();
    }

    /**
     * Real-time entitlement validation endpoint.
     * Protected: Requires user authentication and enforces subscription ownership (or admin authority).
     */
    @GetMapping("/{id}/validate")
    public SubscriptionValidationDto validateSubscription(
            @PathVariable Long id,
            Principal principal
    ) {
        String authenticatedEmail = requireAuthenticatedEmail(principal);
        SubscriptionValidationDto dto = subscriptionService.validateSubscription(id);

        boolean isAdmin = false;
        if (principal instanceof org.springframework.security.core.Authentication auth && auth.getAuthorities() != null) {
            isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN") || a.getAuthority().equalsIgnoreCase("ADMIN"));
        } else {
            org.springframework.security.core.Authentication contextAuth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (contextAuth != null && contextAuth.getAuthorities() != null) {
                isAdmin = contextAuth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN") || a.getAuthority().equalsIgnoreCase("ADMIN"));
            }
        }

        if (!isAdmin && dto.getUserEmail() != null && !dto.getUserEmail().equalsIgnoreCase(authenticatedEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot access another user's subscription validation details");
        }
        return dto;
    }

    @PutMapping("/{id}/status")
    public SubscriptionResponse updateStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        return subscriptionService.updateStatus(id, status);
    }

    @PutMapping("/{id}/cancel")
    public SubscriptionResponse cancelSubscription(
            @PathVariable Long id,
            Principal principal,
            @RequestParam(required = false) String email
    ) {
        String authenticatedEmail = requireAuthenticatedEmail(principal);
        if (email != null && !email.isBlank() && !email.equalsIgnoreCase(authenticatedEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot cancel another user's subscription");
        }
        return subscriptionService.cancelSubscription(id, authenticatedEmail);
    }

    @PutMapping("/{id}/billing-cycle")
    public SubscriptionResponse updateBillingCycle(
            @PathVariable Long id,
            @RequestParam String cycle,
            Principal principal,
            @RequestParam(required = false) String email
    ) {
        String authenticatedEmail = requireAuthenticatedEmail(principal);
        if (email != null && !email.isBlank() && !email.equalsIgnoreCase(authenticatedEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot update another user's subscription");
        }
        BillingCycle newCycle = BillingCycle.valueOf(cycle.toUpperCase());
        return subscriptionService.updateBillingCycle(id, authenticatedEmail, newCycle);
    }

    private String requireAuthenticatedEmail(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User authentication required");
        }
        return principal.getName();
    }
}


