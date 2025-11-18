package com.subscribly.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private String plan;

    private Double price;

    @Enumerated(EnumType.STRING)
    private BillingCycle billingCycle;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;

    private LocalDate startDate;

    // <-- NEW: when subscription is scheduled to end (next renewal boundary)
    private LocalDate endDate;
}
