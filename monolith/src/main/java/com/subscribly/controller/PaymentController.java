package com.subscribly.controller;

import com.subscribly.dto.CreatePaymentOrderRequest;
import com.subscribly.dto.CreatePaymentOrderResponse;
import com.subscribly.dto.PaymentResponse;
import com.subscribly.entity.Payment;
import com.subscribly.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * GATEWAY FLOW: Create server-side payment order for sandbox provider.
     */
    @PostMapping("/order")
    public ResponseEntity<?> createOrder(
            @Valid @RequestBody CreatePaymentOrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Principal principal
    ) {
        String authenticatedEmail = requireAuthenticatedEmail(principal);
        String key = (idempotencyKey != null && !idempotencyKey.isBlank()) ? idempotencyKey : request.getIdempotencyKey();
        try {
            CreatePaymentOrderResponse response = paymentService.createPaymentOrder(
                    request.getSubscriptionId(),
                    authenticatedEmail,
                    request.getCurrency(),
                    key
            );
            return ResponseEntity.ok(response);
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", "ERROR",
                    "message", ex.getMessage()
            ));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", "ERROR",
                    "message", ex.getMessage()
            ));
        }
    }

    /**
     * GATEWAY FLOW: Authenticate and process provider webhook notifications.
     * Verifies cryptographic HMAC-SHA256 signature and applies database-backed idempotency.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signatureHeader,
            @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventIdHeader
    ) {
        try {
            Payment payment = paymentService.processGatewayWebhook(rawPayload, signatureHeader, eventIdHeader);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "paymentId", payment.getId() != null ? payment.getId() : 0L,
                    "paymentStatus", payment.getStatus().name(),
                    "gatewayOrderId", payment.getGatewayOrderId() != null ? payment.getGatewayOrderId() : ""
            ));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", "ERROR",
                    "message", ex.getMessage()
            ));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", "ERROR",
                    "message", ex.getMessage()
            ));
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Webhook processed concurrently by winning transaction"
            ));
        }
    }

    /**
     * SIMULATED FLOW: Make direct manual payment for subscription with idempotency key.
     * Maintained for internal testing/simulation, separate from verified gateway webhooks.
     */
    @PostMapping("/pay/{subscriptionId}")
    public PaymentResponse makePayment(
            @PathVariable Long subscriptionId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Principal principal
    ) {
        String authenticatedEmail = requireAuthenticatedEmail(principal);
        Payment payment = paymentService.makePayment(subscriptionId, authenticatedEmail, idempotencyKey);
        return PaymentResponse.fromEntity(payment);
    }

    /**
     * USER: Get payments for logged-in user
     */
    @GetMapping("/my")
    public List<PaymentResponse> getMyPayments(Principal principal) {
        String authenticatedEmail = requireAuthenticatedEmail(principal);
        return paymentService.getPaymentsByUser(authenticatedEmail).stream()
                .map(PaymentResponse::fromEntity)
                .toList();
    }

    /**
     * ADMIN: Get all payments in the system
     */
    @GetMapping("/all")
    public List<PaymentResponse> getAllPayments() {
        return paymentService.getAllPayments().stream()
                .map(PaymentResponse::fromEntity)
                .toList();
    }

    private String requireAuthenticatedEmail(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User authentication required");
        }
        return principal.getName();
    }
}
