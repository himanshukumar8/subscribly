package com.subscribly.paymentservice.dto;

import com.subscribly.paymentservice.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private String paymentReference;
    private String gatewayOrderId;
    private String gatewayPaymentId;
    private Long subscriptionId;
    private Long userId;
    private Double amount;
    private String currency;
    private PaymentStatus status;
    private int retryCount;
    private LocalDateTime nextRetryAt;
    private String failureReason;
    private LocalDateTime createdAt;
}
