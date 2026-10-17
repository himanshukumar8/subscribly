package com.subscribly.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SubscriptionRenewedEvent extends DomainEvent {
    private Long subscriptionId;
    private Long userId;
    private String planName;
    private String nextBillingDate;

    public SubscriptionRenewedEvent(Long subscriptionId, Long userId, String planName, String nextBillingDate) {
        super("SUBSCRIPTION_RENEWED");
        this.subscriptionId = subscriptionId;
        this.userId = userId;
        this.planName = planName;
        this.nextBillingDate = nextBillingDate;
    }
}
