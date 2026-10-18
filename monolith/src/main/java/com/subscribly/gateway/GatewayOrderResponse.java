package com.subscribly.gateway;

public record GatewayOrderResponse(
        String orderId,
        java.math.BigDecimal amount,
        String currency,
        String status,
        String keyId
) {}
