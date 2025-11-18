package com.subscribly.dto;

import com.subscribly.entity.BillingCycle;
import com.subscribly.entity.SubscriptionStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
public class SubscriptionResponse {
    private Long id;
    private String serviceName;
    private Double price;
    private BillingCycle billingCycle;
    private SubscriptionStatus status;
    private LocalDate startDate;
    private LocalDate nextBillingDate;
}
