package com.subscribly.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaymentOrderResponse {

    private String paymentReference;
    private String gatewayOrderId;
    private java.math.BigDecimal amount;
    private String currency;
    private String keyId;
    private String provider;
    private String status;
}
