package com.subscribly.dto;

import com.subscribly.entity.BillingCycle;
import lombok.Data;

@Data
public class SubscriptionRequest {

    private String plan;          // Basic, Premium, Pro etc.
    private Double price;         // price of the plan
    private BillingCycle billingCycle;  // MONTHLY, YEARLY
}
