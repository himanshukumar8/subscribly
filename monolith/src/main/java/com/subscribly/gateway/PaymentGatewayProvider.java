package com.subscribly.gateway;

public interface PaymentGatewayProvider {

    GatewayOrderResponse createOrder(String paymentReference, Double amount, String currency);

    boolean verifyWebhookSignature(String payload, String signatureHeader);

    boolean verifyPaymentSignature(String orderId, String paymentId, String signature);

    String getProviderName();

    String getKeyId();
}
