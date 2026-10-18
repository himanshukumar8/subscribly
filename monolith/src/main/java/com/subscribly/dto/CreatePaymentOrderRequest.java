package com.subscribly.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaymentOrderRequest {

    @NotNull(message = "Subscription ID is required")
    private Long subscriptionId;

    private String currency;

    private String idempotencyKey;
}
