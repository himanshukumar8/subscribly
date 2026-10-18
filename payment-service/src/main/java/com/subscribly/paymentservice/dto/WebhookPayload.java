package com.subscribly.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookPayload {
    private String eventId;
    private String eventType; // e.g. "payment.captured", "payment.failed"
    private String orderId;
    private String paymentId;
    private String signature;
    private String failureReason;
}

