package com.subscribly.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "payments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_idempotency_key", columnNames = {"idempotency_key"}),
        @UniqueConstraint(name = "uk_payment_gateway_order_id", columnNames = {"gateway_order_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Idempotency key / transaction reference to guarantee at-most-once processing
    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    // Subscription this payment is for
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "user"})
    private Subscription subscription;

    // User this payment belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password"})
    private User user;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal amount;

    @Builder.Default
    @Column(name = "currency", length = 10)
    private String currency = "INR";

    // Gateway Provider References
    @Column(name = "gateway_order_id", length = 128)
    private String gatewayOrderId;

    @Column(name = "gateway_payment_id", length = 128)
    private String gatewayPaymentId;

    @Column(name = "gateway_signature", length = 256)
    private String gatewaySignature;

    @Builder.Default
    @Column(name = "provider", length = 32)
    private String provider = "SIMULATED";

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    // number of attempts already performed
    private int attempts;

    // when this payment was created (date)
    private LocalDate createdAt;

    // next retry date (for legacy date queries)
    private LocalDate nextRetryAt;

    // Precise timestamps for scheduling and auditing
    @Column(name = "created_at_instant")
    private Instant createdAtInstant;

    @Column(name = "next_retry_instant")
    private Instant nextRetryInstant;

    @Column(name = "processed_at_instant")
    private Instant processedAtInstant;

    @Column(name = "failure_reason", length = 512)
    private String failureReason;

    // marker whether this was auto-renew
    @Column(name = "auto_renew")
    private boolean autoRenew;

    @Version
    @Builder.Default
    private Long version = 0L;
}
