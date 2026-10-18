package com.subscribly.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PaymentCompletedEvent extends DomainEvent {
    private Long paymentId;
    private Long subscriptionId;
    private Long userId;
    private Double amount;
    private String transactionReference;

    public PaymentCompletedEvent(Long paymentId, Long subscriptionId, Long userId, Double amount, String transactionReference) {
        super("PAYMENT_COMPLETED");
        this.paymentId = paymentId;
        this.subscriptionId = subscriptionId;
        this.userId = userId;
        this.amount = amount;
        this.transactionReference = transactionReference;
    }
}
