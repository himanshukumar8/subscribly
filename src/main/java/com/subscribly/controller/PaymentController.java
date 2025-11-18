package com.subscribly.controller;

import com.subscribly.entity.Payment;
import com.subscribly.service.PaymentService;
import com.subscribly.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserService userService;

    /**
     * USER: Make manual payment for subscription
     */
    @PostMapping("/pay/{subscriptionId}")
    public Payment makePayment(
            @PathVariable Long subscriptionId,
            @AuthenticationPrincipal UserDetails user
    ) {
        return paymentService.makePayment(subscriptionId, user.getUsername());
    }

    /**
     * USER: Get payments for logged-in user
     */
    @GetMapping("/my")
    public List<Payment> getMyPayments(@AuthenticationPrincipal UserDetails user) {
        return paymentService.getPaymentsByUser(user.getUsername());
    }

    /**
     * ADMIN: Get all payments in the system
     */
    @GetMapping("/all")
    public List<Payment> getAllPayments() {
        return paymentService.getAllPayments();
    }
}
