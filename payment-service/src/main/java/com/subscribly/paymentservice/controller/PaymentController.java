package com.subscribly.paymentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subscribly.paymentservice.dto.CreateOrderRequest;
import com.subscribly.paymentservice.dto.PaymentResponse;
import com.subscribly.paymentservice.dto.VerifyPaymentRequest;
import com.subscribly.paymentservice.dto.WebhookPayload;
import com.subscribly.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @PostMapping("/order")
    public ResponseEntity<PaymentResponse> createOrder(
            @AuthenticationPrincipal String username,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        log.info("[PaymentAPI] Create order request from user: {}", username);
        return ResponseEntity.ok(paymentService.createOrder(username, request));
    }

    @PostMapping("/verify")
    public ResponseEntity<PaymentResponse> verifyPayment(
            @AuthenticationPrincipal String username,
            @Valid @RequestBody VerifyPaymentRequest request
    ) {
        log.info("[PaymentAPI] Verify payment request from user: {}", username);
        return ResponseEntity.ok(paymentService.verifyPayment(username, request));
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signatureHeader
    ) {
        WebhookPayload webhookData = null;
        try {
            webhookData = objectMapper.readValue(rawPayload, WebhookPayload.class);
        } catch (Exception e) {
            log.warn("[PaymentAPI] Could not parse WebhookPayload from rawPayload: {}", e.getMessage());
        }
        log.info("[PaymentAPI] Received webhook notification: {}", webhookData != null ? webhookData.getEventId() : "N/A");
        boolean processed = paymentService.processWebhook(rawPayload, signatureHeader, webhookData);
        if (processed) {
            return ResponseEntity.ok("WEBHOOK_PROCESSED_IDEMPOTENTLY");
        } else {
            return ResponseEntity.badRequest().body("WEBHOOK_FAILED");
        }
    }

    @GetMapping("/my")
    public ResponseEntity<List<PaymentResponse>> getMyPayments(
            @AuthenticationPrincipal String username
    ) {
        return ResponseEntity.ok(paymentService.getMyPayments(username));
    }
}
