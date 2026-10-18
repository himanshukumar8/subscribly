package com.subscribly.gateway;

public record GatewayOrderResponse(
        String orderId,
        Double amount,
        String currency,
        String status,
        String keyId
) {}
