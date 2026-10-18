package com.subscribly.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SubscriptionCreatedEvent extends DomainEvent {
    private Long subscriptionId;
    private Long userId;
    private String planName;
    private String billingCycle;
    private Double price;

    public SubscriptionCreatedEvent(Long subscriptionId, Long userId, String planName, String billingCycle, Double price) {
        super("SUBSCRIPTION_CREATED");
        this.subscriptionId = subscriptionId;
        this.userId = userId;
        this.planName = planName;
        this.billingCycle = billingCycle;
        this.price = price;
    }
}
