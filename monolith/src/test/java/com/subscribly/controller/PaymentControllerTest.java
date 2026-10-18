package com.subscribly.controller;

import com.subscribly.dto.CreatePaymentOrderResponse;
import com.subscribly.entity.Payment;
import com.subscribly.entity.PaymentStatus;
import com.subscribly.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    @DisplayName("POST /api/v1/payments/pay/{id} with Idempotency-Key header passes key to service")
    void testMakePaymentWithIdempotencyKey() throws Exception {
        String key = "client-req-999";

        Payment payment = Payment.builder()
                .id(1L)
                .idempotencyKey(key)
                .amount(java.math.BigDecimal.valueOf(29.99))
                .status(PaymentStatus.SUCCESS)
                .attempts(1)
                .build();

        when(paymentService.makePayment(eq(10L), eq("alice@example.com"), eq(key)))
                .thenReturn(payment);

        mockMvc.perform(post("/api/v1/payments/pay/10")
                        .principal(() -> "alice@example.com")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.idempotencyKey").value(key))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/pay/{id} returns 401 Unauthorized when unauthenticated")
    void testMakePaymentUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/payments/pay/10")
                        .header("Idempotency-Key", "key-123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/payments/my returns payments for authenticated principal")
    void testGetMyPaymentsAuthenticated() throws Exception {
        Payment payment = Payment.builder()
                .id(5L)
                .amount(java.math.BigDecimal.valueOf(49.99))
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentService.getPaymentsByUser("alice@example.com"))
                .thenReturn(List.of(payment));

        mockMvc.perform(get("/api/v1/payments/my")
                        .principal(() -> "alice@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].amount").value(49.99));
    }

    @Test
    @DisplayName("GET /api/v1/payments/my returns 401 Unauthorized when unauthenticated")
    void testGetMyPaymentsUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/payments/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/payments/order creates server-side order and returns 200 OK")
    void testCreatePaymentOrderAuthenticated() throws Exception {
        CreatePaymentOrderResponse orderResponse = CreatePaymentOrderResponse.builder()
                .paymentReference("PAY-REF-ABC-123")
                .gatewayOrderId("order_rzp_test_456")
                .amount(java.math.BigDecimal.valueOf(49.99))
                .currency("INR")
                .keyId("rzp_test_key")
                .provider("RAZORPAY")
                .status("PENDING")
                .build();

        when(paymentService.createPaymentOrder(eq(10L), eq("alice@example.com"), eq("INR"), any()))
                .thenReturn(orderResponse);

        String jsonRequest = """
                {
                    "subscriptionId": 10,
                    "currency": "INR",
                    "idempotencyKey": "order-req-001"
                }
                """;

        mockMvc.perform(post("/api/v1/payments/order")
                        .principal(() -> "alice@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gatewayOrderId").value("order_rzp_test_456"))
                .andExpect(jsonPath("$.paymentReference").value("PAY-REF-ABC-123"))
                .andExpect(jsonPath("$.amount").value(49.99))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/order returns 401 when unauthenticated")
    void testCreatePaymentOrderUnauthenticatedReturns401() throws Exception {
        String jsonRequest = "{\"subscriptionId\": 10}";
        mockMvc.perform(post("/api/v1/payments/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/payments/webhook with valid signature returns 200 SUCCESS")
    void testWebhookEndpointSuccess() throws Exception {
        Payment payment = Payment.builder()
                .id(77L)
                .gatewayOrderId("order_test_999")
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentService.processGatewayWebhook(any(), eq("valid_sig"), any()))
                .thenReturn(payment);

        String rawPayload = "{\"eventId\":\"evt_1\",\"orderId\":\"order_test_999\"}";

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("X-Razorpay-Signature", "valid_sig")
                        .header("X-Razorpay-Event-Id", "evt_1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rawPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.paymentId").value(77))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/webhook with invalid signature returns 400 Bad Request")
    void testWebhookEndpointInvalidSignatureReturns400() throws Exception {
        when(paymentService.processGatewayWebhook(any(), eq("invalid_sig"), any()))
                .thenThrow(new SecurityException("Invalid webhook signature header"));

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("X-Razorpay-Signature", "invalid_sig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Invalid webhook signature header"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/order returns 403 Forbidden when user is unauthorized for subscription")
    void testCreatePaymentOrderUnauthorizedReturns403() throws Exception {
        when(paymentService.createPaymentOrder(eq(10L), eq("attacker@example.com"), any(), any()))
                .thenThrow(new SecurityException("Unauthorized: Subscription does not belong to user"));

        mockMvc.perform(post("/api/v1/payments/order")
                        .principal(() -> "attacker@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subscriptionId\": 10}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Unauthorized: Subscription does not belong to user"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/order returns 400 Bad Request on cancelled subscription or key conflict")
    void testCreatePaymentOrderConflictReturns400() throws Exception {
        when(paymentService.createPaymentOrder(eq(10L), eq("alice@example.com"), any(), any()))
                .thenThrow(new IllegalStateException("Cannot create payment order for a CANCELLED subscription."));

        mockMvc.perform(post("/api/v1/payments/order")
                        .principal(() -> "alice@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subscriptionId\": 10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Cannot create payment order for a CANCELLED subscription."));
    }

    @Test
    @DisplayName("POST /api/v1/payments/webhook handles concurrent optimistic locking race cleanly with 200 SUCCESS")
    void testWebhookEndpointConcurrentRaceReturns200() throws Exception {
        when(paymentService.processGatewayWebhook(any(), any(), any()))
                .thenThrow(new org.springframework.orm.ObjectOptimisticLockingFailureException(
                        "Payment", "100"
                ));

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("X-Razorpay-Signature", "sig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Webhook processed concurrently by winning transaction"));
    }
}
