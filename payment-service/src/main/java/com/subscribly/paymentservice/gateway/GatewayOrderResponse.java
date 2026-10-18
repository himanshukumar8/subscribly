package com.subscribly.paymentservice.gateway;

public record GatewayOrderResponse(
        String orderId,
        Double amount,
        String currency,
        String status
) {}
