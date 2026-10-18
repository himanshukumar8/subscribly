package com.subscribly.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PaymentFailedEvent extends DomainEvent {
    private Long paymentId;
    private Long subscriptionId;
    private Long userId;
    private Double amount;
    private String failureReason;

    public PaymentFailedEvent(Long paymentId, Long subscriptionId, Long userId, Double amount, String failureReason) {
        super("PAYMENT_FAILED");
        this.paymentId = paymentId;
        this.subscriptionId = subscriptionId;
        this.userId = userId;
        this.amount = amount;
        this.failureReason = failureReason;
    }
}
