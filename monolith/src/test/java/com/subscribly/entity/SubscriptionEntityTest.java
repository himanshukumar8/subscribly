package com.subscribly.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class SubscriptionEntityTest {

    @Test
    @DisplayName("Subscription is active before exact expiry timestamp")
    void testActiveBeforeExpiryTimestamp() {
        Instant now = Instant.now();
        Instant futureExpiry = now.plus(1, ChronoUnit.HOURS);

        Subscription sub = Subscription.builder()
                .status(SubscriptionStatus.ACTIVE)
                .startedAt(now.minus(23, ChronoUnit.HOURS))
                .expiresAt(futureExpiry)
                .build();

        assertFalse(sub.isExpired(now), "Subscription should not be expired before expiry timestamp");
        assertEquals(SubscriptionStatus.ACTIVE, sub.getEffectiveStatus(now));
        assertTrue(sub.hasActiveEntitlement(now), "User should have active entitlement");
    }

    @Test
    @DisplayName("Subscription is immediately expired when exact timestamp has passed (Rule 6)")
    void testImmediatelyExpiredAfterTimestamp() {
        Instant now = Instant.now();
        Instant pastExpiry = now.minus(5, ChronoUnit.SECONDS); // Expired 5 seconds ago

        Subscription sub = Subscription.builder()
                .status(SubscriptionStatus.ACTIVE) // Still marked ACTIVE in DB
                .startedAt(now.minus(30, ChronoUnit.DAYS))
                .expiresAt(pastExpiry)
                .build();

        assertTrue(sub.isExpired(now), "Subscription must be expired immediately once exact timestamp passes");
        assertEquals(SubscriptionStatus.EXPIRED, sub.getEffectiveStatus(now),
                "Effective status must dynamically evaluate to EXPIRED without waiting for a midnight batch job");
        assertFalse(sub.hasActiveEntitlement(now), "User must NOT have active entitlement past expiry timestamp");
    }

    @Test
    @DisplayName("Subscription is expired at the exact timestamp boundary (now == expiresAt)")
    void testExactExpiryTimestampBoundary() {
        Instant exactExpiry = Instant.parse("2026-10-17T12:00:00Z");

        Subscription sub = Subscription.builder()
                .status(SubscriptionStatus.ACTIVE)
                .startedAt(exactExpiry.minus(30, ChronoUnit.DAYS))
                .expiresAt(exactExpiry)
                .build();

        // At the exact boundary instant, subscription period has ended
        assertTrue(sub.isExpired(exactExpiry), "Subscription must be expired at exact boundary instant");
        assertEquals(SubscriptionStatus.EXPIRED, sub.getEffectiveStatus(exactExpiry));
        assertFalse(sub.hasActiveEntitlement(exactExpiry));

        // 1 millisecond before boundary, subscription is still active
        Instant oneMilliBefore = exactExpiry.minusMillis(1);
        assertFalse(sub.isExpired(oneMilliBefore), "Subscription must still be active 1ms before expiry");
        assertTrue(sub.hasActiveEntitlement(oneMilliBefore));
    }

    @Test
    @DisplayName("Cancelled subscription retains CANCELLED status even if expiry is in future")
    void testCancelledSubscriptionRetainsCancelledStatus() {
        Instant now = Instant.now();
        Subscription sub = Subscription.builder()
                .status(SubscriptionStatus.CANCELLED)
                .expiresAt(now.plus(10, ChronoUnit.DAYS))
                .build();

        assertEquals(SubscriptionStatus.CANCELLED, sub.getEffectiveStatus(now));
        assertFalse(sub.hasActiveEntitlement(now));
    }

    @Test
    @DisplayName("Legacy subscription with only endDate falls back to end of day UTC")
    void testLegacyEndDateFallback() {
        LocalDate today = LocalDate.now();
        Subscription sub = Subscription.builder()
                .status(SubscriptionStatus.ACTIVE)
                .endDate(today)
                .expiresAt(null)
                .build();

        Instant effectiveExpiry = sub.getEffectiveExpiryInstant();
        assertNotNull(effectiveExpiry, "Should calculate fallback expiry from endDate");

        // Way before end-of-day
        Instant morning = today.atTime(8, 0).atZone(java.time.ZoneOffset.UTC).toInstant();
        assertFalse(sub.isExpired(morning));

        // After end-of-day
        Instant nextDay = today.plusDays(1).atStartOfDay().atZone(java.time.ZoneOffset.UTC).toInstant();
        assertTrue(sub.isExpired(nextDay));
    }

    @Test
    @DisplayName("setPeriod synchronizes timestamps and dates in lockstep")
    void testSetPeriodSynchronizesFields() {
        Instant start = Instant.parse("2026-10-17T12:00:00Z");
        Subscription sub = new Subscription();
        sub.setPeriod(start, BillingCycle.MONTHLY);

        assertEquals(start, sub.getStartedAt());
        assertEquals(LocalDate.of(2026, 10, 17), sub.getStartDate());
        assertEquals(Instant.parse("2026-11-17T12:00:00Z"), sub.getExpiresAt());
        assertEquals(LocalDate.of(2026, 11, 17), sub.getEndDate());
    }
}
