package com.subscribly.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SubscriptionExpiredEvent extends DomainEvent {
    private Long subscriptionId;
    private Long userId;
    private String planName;

    public SubscriptionExpiredEvent(Long subscriptionId, Long userId, String planName) {
        super("SUBSCRIPTION_EXPIRED");
        this.subscriptionId = subscriptionId;
        this.userId = userId;
        this.planName = planName;
    }
}
