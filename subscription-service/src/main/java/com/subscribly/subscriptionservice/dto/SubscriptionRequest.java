package com.subscribly.subscriptionservice.dto;

import com.subscribly.subscriptionservice.entity.BillingCycle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubscriptionRequest {

    @NotBlank(message = "Plan is required")
    private String plan;

    @NotNull(message = "Price is required")
    private Double price;

    @NotNull(message = "Billing cycle is required")
    private BillingCycle billingCycle;
}
