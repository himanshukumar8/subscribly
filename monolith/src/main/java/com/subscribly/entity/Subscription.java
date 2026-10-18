package com.subscribly.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

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

    // Calendar date representations for backwards compatibility
    private LocalDate startDate;

    private LocalDate endDate;

    // Exact timestamp fields (authoritative source of truth for expiry)
    private Instant startedAt;

    private Instant expiresAt;

    @Builder.Default
    private boolean autoRenew = true;

    @Version
    @Builder.Default
    private Long version = 0L;

    /**
     * Resolves the authoritative expiry timestamp, falling back to end-of-day UTC for legacy records.
     */
    public Instant getEffectiveExpiryInstant() {
        if (expiresAt != null) {
            return expiresAt;
        }
        if (endDate != null) {
            return endDate.atTime(23, 59, 59).atZone(ZoneOffset.UTC).toInstant();
        }
        return null;
    }

    /**
     * Returns true if the subscription has expired relative to the given instant.
     */
    public boolean isExpired(Instant now) {
        Instant expiry = getEffectiveExpiryInstant();
        return expiry != null && !now.isBefore(expiry);
    }

    /**
     * Evaluates real-time dynamic status without waiting for batch jobs.
     */
    public SubscriptionStatus getEffectiveStatus(Instant now) {
        if (status == SubscriptionStatus.CANCELLED) {
            return SubscriptionStatus.CANCELLED;
        }
        if (status == SubscriptionStatus.PAUSED) {
            return SubscriptionStatus.PAUSED;
        }
        if (isExpired(now)) {
            return SubscriptionStatus.EXPIRED;
        }
        return status != null ? status : SubscriptionStatus.ACTIVE;
    }

    /**
     * Returns true if the user currently holds active subscription entitlement.
     */
    public boolean hasActiveEntitlement(Instant now) {
        return getEffectiveStatus(now) == SubscriptionStatus.ACTIVE;
    }

    /**
     * Synchronizes timestamp and date boundaries for a given billing cycle.
     */
    public void setPeriod(Instant start, BillingCycle cycle) {
        this.startedAt = start;
        this.startDate = LocalDate.ofInstant(start, ZoneOffset.UTC);
        int months = cycle != null ? cycle.getMonths() : 1;
        this.expiresAt = start.atZone(ZoneOffset.UTC).plusMonths(months).toInstant();
        this.endDate = LocalDate.ofInstant(this.expiresAt, ZoneOffset.UTC);
    }
}

