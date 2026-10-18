package com.subscribly.controller;

import com.subscribly.dto.SubscriptionResponse;
import com.subscribly.dto.UserResponse;
import com.subscribly.service.SubscriptionService;
import com.subscribly.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/admin", "/api/admin"})
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
public class AdminController {

    private final SubscriptionService subscriptionService;
    private final UserService userService;

    @GetMapping("/subscriptions")
    public ResponseEntity<List<SubscriptionResponse>> getAllSubscriptions() {
        return ResponseEntity.ok(subscriptionService.getAllSubscriptions());
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable Long id,
            @RequestParam String role,
            Authentication authentication
    ) {
        String adminEmail = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(userService.updateUserRole(id, role, adminEmail));
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable Long id,
            @RequestParam String status,
            Authentication authentication
    ) {
        String adminEmail = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(userService.updateUserStatus(id, status, adminEmail));
    }
}
