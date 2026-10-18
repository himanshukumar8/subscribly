package com.subscribly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.subscribly.entity.Payment;
import com.subscribly.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse {

    private Long id;
    private String idempotencyKey;
    private Long subscriptionId;
    private Long userId;
    private String userEmail;
    private BigDecimal amount;
    private String currency;
    private String gatewayOrderId;
    private String gatewayPaymentId;
    private String provider;
    private PaymentStatus status;
    private int attempts;
    private LocalDate createdAt;
    private LocalDate nextRetryAt;
    private Instant createdAtInstant;
    private Instant nextRetryInstant;
    private Instant processedAtInstant;
    private String failureReason;
    private boolean autoRenew;

    /**
     * Backward-compatible alias for frontend clients expecting paymentReference.
     */
    public String getPaymentReference() {
        return idempotencyKey;
    }

    /**
     * Backward-compatible alias for frontend clients expecting retryCount.
     */
    public int getRetryCount() {
        return attempts;
    }

    /**
     * Maps a Payment entity to a PaymentResponse DTO, eliminating JPA proxy and cyclic references.
     */
    public static PaymentResponse fromEntity(Payment payment) {
        if (payment == null) {
            return null;
        }

        Long subId = null;
        if (payment.getSubscription() != null) {
            subId = payment.getSubscription().getId();
        }

        Long uId = null;
        String uEmail = null;
        if (payment.getUser() != null) {
            uId = payment.getUser().getId();
            uEmail = payment.getUser().getEmail();
        }

        return PaymentResponse.builder()
                .id(payment.getId())
                .idempotencyKey(payment.getIdempotencyKey())
                .subscriptionId(subId)
                .userId(uId)
                .userEmail(uEmail)
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .gatewayOrderId(payment.getGatewayOrderId())
                .gatewayPaymentId(payment.getGatewayPaymentId())
                .provider(payment.getProvider())
                .status(payment.getStatus())
                .attempts(payment.getAttempts())
                .createdAt(payment.getCreatedAt())
                .nextRetryAt(payment.getNextRetryAt())
                .createdAtInstant(payment.getCreatedAtInstant())
                .nextRetryInstant(payment.getNextRetryInstant())
                .processedAtInstant(payment.getProcessedAtInstant())
                .failureReason(payment.getFailureReason())
                .autoRenew(payment.isAutoRenew())
                .build();
    }
}
