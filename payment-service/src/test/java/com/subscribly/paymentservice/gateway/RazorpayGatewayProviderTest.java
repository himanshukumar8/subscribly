package com.subscribly.paymentservice.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class RazorpayGatewayProviderTest {

    private RazorpayGatewayProvider provider;

    private static final String KEY_ID = "rzp_test_mockKeyId";
    private static final String KEY_SECRET = "rzp_test_mockSecretKey12345";
    private static final String WEBHOOK_SECRET = "rzp_test_mockWebhookSecret12345";

    @BeforeEach
    void setUp() {
        provider = new RazorpayGatewayProvider();
        ReflectionTestUtils.setField(provider, "keyId", KEY_ID);
        ReflectionTestUtils.setField(provider, "keySecret", KEY_SECRET);
        ReflectionTestUtils.setField(provider, "webhookSecret", WEBHOOK_SECRET);
    }

    @Test
    @DisplayName("Create Order: Generates gateway order ID prefixed with 'order_'")
    void testCreateOrder() {
        GatewayOrderResponse response = provider.createOrder("PAY-REF-001", 100.0, "INR");
        assertNotNull(response);
        assertNotNull(response.orderId());
        assertTrue(response.orderId().startsWith("order_"));
        assertEquals(100.0, response.amount());
        assertEquals("INR", response.currency());
        assertEquals("created", response.status());
    }

    @Test
    @DisplayName("Webhook Verification: Valid HMAC-SHA256 signature returns true using constant-time comparison")
    void testVerifyWebhookSignatureValid() {
        String payload = "{\"event\":\"payment.captured\",\"orderId\":\"order_123\"}";
        // Calculate expected signature
        String generatedSignature = ReflectionTestUtils.invokeMethod(provider, "calculateHmacSha256", payload, WEBHOOK_SECRET);
        assertNotNull(generatedSignature);

        assertTrue(provider.verifyWebhookSignature(payload, generatedSignature));
    }

    @Test
    @DisplayName("Webhook Verification: Invalid signature or tampered payload returns false")
    void testVerifyWebhookSignatureInvalid() {
        String payload = "{\"event\":\"payment.captured\",\"orderId\":\"order_123\"}";
        assertFalse(provider.verifyWebhookSignature(payload, "invalid_signature_hex"));
        assertFalse(provider.verifyWebhookSignature(null, "some_sig"));
        assertFalse(provider.verifyWebhookSignature(payload, null));
    }

    @Test
    @DisplayName("Payment Signature: Valid HMAC-SHA256 on order_id|payment_id returns true")
    void testVerifyPaymentSignatureValid() {
        String orderId = "order_abc";
        String paymentId = "pay_xyz";
        String payload = orderId + "|" + paymentId;
        String generatedSignature = ReflectionTestUtils.invokeMethod(provider, "calculateHmacSha256", payload, KEY_SECRET);

        assertTrue(provider.verifyPaymentSignature(orderId, paymentId, generatedSignature));
    }
}
