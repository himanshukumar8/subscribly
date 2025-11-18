package com.subscribly.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Subscription this payment is for
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    // NEW: user this payment belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private Double amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    // number of attempts already performed
    private int attempts;

    // when this payment was created
    private LocalDate createdAt;

    // next retry date (for retry logic)
    private LocalDate nextRetryAt;

    // marker whether this was auto-renew
    private boolean autoRenew;
}
