package com.subscribly.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("plan")
    @JsonAlias({"planName"})
    private String plan;                // Basic, Premium, Pro etc.

    @JsonProperty("price")
    @JsonAlias({"amount"})
    private Double price;               // price of the plan

    private BillingCycle billingCycle;  // MONTHLY, QUARTERLY, YEARLY
    private Boolean autoRenew;          // defaults to true if null
    private String email;               // optional explicit user email
}

