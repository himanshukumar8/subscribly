package com.subscribly.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.subscribly.dto.PaymentResponse;
import com.subscribly.entity.Payment;
import com.subscribly.entity.PaymentStatus;
import com.subscribly.entity.Subscription;
import com.subscribly.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PaymentResponseDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("DTO Mapping: Maps Payment entity to PaymentResponse without exposing internal User entity or password")
    void testPaymentResponseMappingPreventsEntityExposure() throws Exception {
        User user = User.builder()
                .id(42L)
                .email("secure@example.com")
                .fullName("Secure User")
                .password("$2a$12$SuperSecretHashedPasswordNeverExpose")
                .build();

        Subscription subscription = Subscription.builder()
                .id(101L)
                .user(user)
                .build();

        Payment payment = Payment.builder()
                .id(9001L)
                .idempotencyKey("PAY-REF-SECURE-999")
                .subscription(subscription)
                .user(user)
                .amount(new BigDecimal("49.99"))
                .currency("INR")
                .gatewayOrderId("order_rzp_secure_123")
                .gatewayPaymentId("pay_rzp_secure_456")
                .provider("RAZORPAY")
                .status(PaymentStatus.SUCCESS)
                .attempts(1)
                .createdAt(LocalDate.of(2026, 10, 18))
                .createdAtInstant(Instant.parse("2026-10-18T12:00:00Z"))
                .processedAtInstant(Instant.parse("2026-10-18T12:01:00Z"))
                .autoRenew(false)
                .build();

        PaymentResponse response = PaymentResponse.fromEntity(payment);

        assertNotNull(response);
        assertEquals(9001L, response.getId());
        assertEquals("PAY-REF-SECURE-999", response.getIdempotencyKey());
        assertEquals("PAY-REF-SECURE-999", response.getPaymentReference(), "Alias getter must match");
        assertEquals(101L, response.getSubscriptionId());
        assertEquals(42L, response.getUserId());
        assertEquals("secure@example.com", response.getUserEmail());
        assertEquals(0, response.getAmount().compareTo(new BigDecimal("49.99")));
        assertEquals("INR", response.getCurrency());
        assertEquals("order_rzp_secure_123", response.getGatewayOrderId());
        assertEquals(1, response.getRetryCount(), "Alias getter for attempts");

        // Verify JSON serialization does not contain password or user object
        String json = objectMapper.writeValueAsString(response);
        assertFalse(json.contains("SuperSecretHashedPasswordNeverExpose"), "Hashed password must NEVER leak into response JSON");
        assertFalse(json.contains("\"user\":"), "Internal User entity must not be nested in JSON");
        assertFalse(json.contains("hibernateLazyInitializer"), "ByteBuddy proxies must not be serialized");
        assertTrue(json.contains("\"userEmail\":\"secure@example.com\""));
        assertTrue(json.contains("\"subscriptionId\":101"));
        assertTrue(json.contains("\"paymentReference\":\"PAY-REF-SECURE-999\""));
    }

    @Test
    @DisplayName("DTO Mapping: Null payment entity returns null safely")
    void testNullEntityHandling() {
        assertNull(PaymentResponse.fromEntity(null));
    }
}
