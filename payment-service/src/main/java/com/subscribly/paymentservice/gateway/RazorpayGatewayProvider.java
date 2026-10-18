package com.subscribly.paymentservice.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Component
public class RazorpayGatewayProvider implements PaymentGatewayProvider {

    @Value("${razorpay.key-id:rzp_test_mockKeyId}")
    private String keyId;

    @Value("${razorpay.key-secret:rzp_test_mockSecretKey12345}")
    private String keySecret;

    @Value("${razorpay.webhook-secret:rzp_test_mockWebhookSecret12345}")
    private String webhookSecret;

    private static final String HMAC_SHA256 = "HmacSHA256";

    @Override
    public GatewayOrderResponse createOrder(String paymentReference, Double amount, String currency) {
        log.info("[RazorpayGateway] Initiating Order Creation for ref: {}, amount: {} {}", paymentReference, amount, currency);
        // In production, this invokes Razorpay API: POST https://api.razorpay.com/v1/orders
        String gatewayOrderId = "order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        log.info("[RazorpayGateway] Order created successfully. Razorpay Order ID: {}", gatewayOrderId);
        return new GatewayOrderResponse(gatewayOrderId, amount, currency, "created");
    }

    @Override
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        if (orderId == null || paymentId == null || signature == null || signature.isBlank()) {
            return false;
        }
        log.info("[RazorpayGateway] Verifying payment signature for Order: {}, Payment: {}", orderId, paymentId);
        String payload = orderId + "|" + paymentId;
        String generatedSignature = calculateHmacSha256(payload, keySecret);
        byte[] expectedBytes = generatedSignature.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = signature.trim().getBytes(StandardCharsets.UTF_8);
        boolean isValid = java.security.MessageDigest.isEqual(expectedBytes, actualBytes);
        if (!isValid) {
            log.warn("[RazorpayGateway] Payment signature mismatch!");
        }
        return isValid;
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signatureHeader) {
        log.info("[RazorpayGateway] Verifying webhook signature header");
        if (payload == null || signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }
        String expectedSignature = calculateHmacSha256(payload, webhookSecret);
        byte[] expectedBytes = expectedSignature.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = signatureHeader.trim().getBytes(StandardCharsets.UTF_8);
        return java.security.MessageDigest.isEqual(expectedBytes, actualBytes);
    }

    @Override
    public String getProviderName() {
        return "RAZORPAY";
    }

    private String calculateHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("[RazorpayGateway] HMAC-SHA256 calculation error: {}", e.getMessage(), e);
            throw new RuntimeException("Cryptographic verification failed", e);
        }
    }
}
