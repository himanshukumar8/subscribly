package com.subscribly.dto;

import com.subscribly.entity.BillingCycle;
import com.subscribly.entity.SubscriptionStatus;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionValidationDto {
    private Long subscriptionId;
    private String userEmail;
    private String plan;
    private Double price;
    private BillingCycle billingCycle;
    private SubscriptionStatus persistedStatus;
    private SubscriptionStatus effectiveStatus;
    private boolean active;
    private Instant startedAt;
    private Instant expiresAt;
    private long remainingSeconds;
    private boolean autoRenew;
}
