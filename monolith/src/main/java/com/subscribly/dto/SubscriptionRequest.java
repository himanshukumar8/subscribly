package com.subscribly.dto;

import com.subscribly.entity.BillingCycle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRequest {

    private String plan;                // Basic, Premium, Pro etc.
    private Double price;               // price of the plan
    private BillingCycle billingCycle;  // MONTHLY, QUARTERLY, YEARLY
    private Boolean autoRenew;          // defaults to true if null
    private String email;               // optional explicit user email
}

