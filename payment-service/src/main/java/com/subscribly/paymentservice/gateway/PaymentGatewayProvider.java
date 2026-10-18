package com.subscribly.paymentservice.gateway;

public interface PaymentGatewayProvider {

    GatewayOrderResponse createOrder(String paymentReference, Double amount, String currency);

    boolean verifyPaymentSignature(String orderId, String paymentId, String signature);

    boolean verifyWebhookSignature(String payload, String signatureHeader);

    String getProviderName();
}
