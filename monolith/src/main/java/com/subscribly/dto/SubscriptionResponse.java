package com.subscribly.dto;

import com.subscribly.entity.BillingCycle;
import com.subscribly.entity.SubscriptionStatus;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {
    private Long id;
    private String plan;
    private String serviceName; // alias for plan
    private Double price;
    private BillingCycle billingCycle;
    private SubscriptionStatus status;
    private SubscriptionStatus effectiveStatus;
    private boolean active;
    private Instant startedAt;
    private Instant expiresAt;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate nextBillingDate; // alias for endDate
    private boolean autoRenew;
    private Long remainingSeconds;
}

