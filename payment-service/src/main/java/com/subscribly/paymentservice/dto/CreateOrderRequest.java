package com.subscribly.paymentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    @NotNull(message = "Subscription ID is required")
    private Long subscriptionId;
    @NotNull(message = "Amount is required")
    private Double amount;
    @NotBlank(message = "Currency is required")
    private String currency;
}
