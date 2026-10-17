package com.subscribly.service;

import com.subscribly.dto.SubscriptionRequest;
import com.subscribly.dto.SubscriptionResponse;
import com.subscribly.dto.SubscriptionValidationDto;
import com.subscribly.entity.*;
import com.subscribly.repository.PaymentRepository;
import com.subscribly.repository.SubscriptionRepository;
import com.subscribly.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .fullName("Test User")
                .build();
    }

    @Test
    @DisplayName("createSubscription populates exact timestamps and default autoRenew")
    void testCreateSubscription() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription s = invocation.getArgument(0);
            s.setId(100L);
            return s;
        });

        SubscriptionRequest req = SubscriptionRequest.builder()
                .plan("PRO")
                .price(49.99)
                .billingCycle(BillingCycle.MONTHLY)
                .autoRenew(true)
                .build();

        SubscriptionResponse response = subscriptionService.createSubscription("user@example.com", req);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("PRO", response.getPlan());
        assertEquals(49.99, response.getPrice());
        assertEquals(SubscriptionStatus.ACTIVE, response.getStatus());
        assertEquals(SubscriptionStatus.ACTIVE, response.getEffectiveStatus());
        assertTrue(response.isActive());
        assertNotNull(response.getStartedAt());
        assertNotNull(response.getExpiresAt());
        assertTrue(response.getExpiresAt().isAfter(response.getStartedAt()));
        assertTrue(response.isAutoRenew());
        verify(subscriptionRepository, times(1)).save(any(Subscription.class));
    }

    @Test
    @DisplayName("validateSubscription returns active entitlement when within valid timestamp window")
    void testValidateSubscriptionActive() {
        Instant now = Instant.now();
        Instant futureExpiry = now.plus(15, ChronoUnit.DAYS);

        Subscription activeSub = Subscription.builder()
                .id(10L)
                .user(testUser)
                .plan("ENTERPRISE")
                .price(99.0)
                .billingCycle(BillingCycle.MONTHLY)
                .status(SubscriptionStatus.ACTIVE)
                .startedAt(now.minus(15, ChronoUnit.DAYS))
                .expiresAt(futureExpiry)
                .autoRenew(true)
                .build();

        when(subscriptionRepository.findById(10L)).thenReturn(Optional.of(activeSub));

        SubscriptionValidationDto result = subscriptionService.validateSubscription(10L);

        assertNotNull(result);
        assertEquals(10L, result.getSubscriptionId());
        assertEquals("user@example.com", result.getUserEmail());
        assertTrue(result.isActive());
        assertEquals(SubscriptionStatus.ACTIVE, result.getEffectiveStatus());
        assertEquals(SubscriptionStatus.ACTIVE, result.getPersistedStatus());
        assertTrue(result.getRemainingSeconds() > 0);

        // No need to reconcile status since it's already active and unexpired
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    @DisplayName("validateSubscription lazily reconciles status to EXPIRED immediately after timestamp passes (Rule 6)")
    void testValidateSubscriptionExpiredLazyReconciliation() {
        Instant now = Instant.now();
        Instant pastExpiry = now.minus(2, ChronoUnit.MINUTES); // Expired 2 minutes ago

        Subscription expiredSub = Subscription.builder()
                .id(20L)
                .user(testUser)
                .plan("BASIC")
                .price(9.99)
                .billingCycle(BillingCycle.MONTHLY)
                .status(SubscriptionStatus.ACTIVE) // DB column still says ACTIVE!
                .startedAt(now.minus(30, ChronoUnit.DAYS))
                .expiresAt(pastExpiry)
                .autoRenew(true)
                .build();

        when(subscriptionRepository.findById(20L)).thenReturn(Optional.of(expiredSub));

        SubscriptionValidationDto result = subscriptionService.validateSubscription(20L);

        assertNotNull(result);
        assertFalse(result.isActive(), "Access must be denied when exact timestamp has passed");
        assertEquals(SubscriptionStatus.EXPIRED, result.getEffectiveStatus(),
                "Effective status must dynamically evaluate to EXPIRED without waiting for batch job");
        assertEquals(SubscriptionStatus.EXPIRED, result.getPersistedStatus());
        assertEquals(0, result.getRemainingSeconds());

        // Verify lazy reconciliation persisted the status transition in DB
        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository, times(1)).save(captor.capture());
        assertEquals(SubscriptionStatus.EXPIRED, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("getSubscriptionsByUser lazily reconciles expired subscriptions on query")
    void testGetSubscriptionsByUserReconcilesExpired() {
        Instant now = Instant.now();

        Subscription sub1 = Subscription.builder()
                .id(1L)
                .user(testUser)
                .plan("PLAN_A")
                .status(SubscriptionStatus.ACTIVE)
                .expiresAt(now.plus(5, ChronoUnit.DAYS))
                .build();

        Subscription sub2 = Subscription.builder()
                .id(2L)
                .user(testUser)
                .plan("PLAN_B")
                .status(SubscriptionStatus.ACTIVE)
                .expiresAt(now.minus(1, ChronoUnit.HOURS)) // Expired
                .build();

        when(subscriptionRepository.findByUserEmail("user@example.com")).thenReturn(List.of(sub1, sub2));

        List<SubscriptionResponse> results = subscriptionService.getSubscriptionsByUser("user@example.com");

        assertEquals(2, results.size());
        assertTrue(results.get(0).isActive());
        assertEquals(SubscriptionStatus.ACTIVE, results.get(0).getEffectiveStatus());

        assertFalse(results.get(1).isActive());
        assertEquals(SubscriptionStatus.EXPIRED, results.get(1).getEffectiveStatus());

        // sub2 must have been saved as EXPIRED
        verify(subscriptionRepository, times(1)).save(sub2);
        assertEquals(SubscriptionStatus.EXPIRED, sub2.getStatus());
    }

    @Test
    @DisplayName("createAutoRenewPaymentForSubscription advances expiry and records payment on autoRenew")
    void testCreateAutoRenewPaymentSuccess() {
        Instant now = Instant.now();
        Instant currentExpiry = now.minus(10, ChronoUnit.SECONDS);

        Subscription sub = Subscription.builder()
                .id(50L)
                .user(testUser)
                .plan("PRO")
                .price(49.99)
                .billingCycle(BillingCycle.MONTHLY)
                .status(SubscriptionStatus.ACTIVE)
                .expiresAt(currentExpiry)
                .autoRenew(true)
                .build();

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(999L);
            return p;
        });

        Payment payment = subscriptionService.createAutoRenewPaymentForSubscription(sub);

        assertNotNull(payment);
        assertEquals(999L, payment.getId());
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertTrue(payment.isAutoRenew());
        assertEquals(49.99, payment.getAmount());

        // Subscription must have updated expiry date in future and active status
        assertEquals(SubscriptionStatus.ACTIVE, sub.getStatus());
        assertTrue(sub.getExpiresAt().isAfter(now));
        verify(subscriptionRepository, times(1)).save(sub);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("createAutoRenewPaymentForSubscription marks EXPIRED when autoRenew is disabled")
    void testCreateAutoRenewPaymentDisabledAutoRenew() {
        Instant now = Instant.now();
        Subscription sub = Subscription.builder()
                .id(51L)
                .user(testUser)
                .plan("PRO")
                .price(49.99)
                .billingCycle(BillingCycle.MONTHLY)
                .status(SubscriptionStatus.ACTIVE)
                .expiresAt(now.minus(1, ChronoUnit.SECONDS))
                .autoRenew(false) // Auto-renew turned off
                .build();

        Payment payment = subscriptionService.createAutoRenewPaymentForSubscription(sub);

        assertNull(payment, "No payment should be generated when autoRenew is disabled");
        assertEquals(SubscriptionStatus.EXPIRED, sub.getStatus(), "Subscription must transition to EXPIRED");
        verify(subscriptionRepository, times(1)).save(sub);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("createAutoRenewPaymentForSubscription returns existing payment on idempotent retry without double extending")
    void testCreateAutoRenewPaymentIdempotentHit() {
        Instant now = Instant.now();
        Instant currentExpiry = now.minus(5, ChronoUnit.SECONDS);

        Subscription sub = Subscription.builder()
                .id(55L)
                .user(testUser)
                .plan("PRO")
                .price(49.99)
                .billingCycle(BillingCycle.MONTHLY)
                .status(SubscriptionStatus.ACTIVE)
                .expiresAt(currentExpiry)
                .autoRenew(true)
                .build();

        Payment existingPayment = Payment.builder()
                .id(888L)
                .idempotencyKey("renew-sub-55-" + currentExpiry.getEpochSecond())
                .status(PaymentStatus.SUCCESS)
                .amount(49.99)
                .build();

        when(paymentRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.of(existingPayment));

        Payment payment = subscriptionService.createAutoRenewPaymentForSubscription(sub);

        assertNotNull(payment);
        assertEquals(888L, payment.getId());
        // Verify subscription expiry was not changed and not saved
        assertEquals(currentExpiry, sub.getExpiresAt());
        verify(subscriptionRepository, never()).save(sub);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("createAutoRenewPaymentForSubscription preserves CANCELLED status and never overwrites to EXPIRED")
    void testCreateAutoRenewCancelledSubscriptionPreserved() {
        Instant now = Instant.now();
        Subscription sub = Subscription.builder()
                .id(56L)
                .user(testUser)
                .plan("PRO")
                .status(SubscriptionStatus.CANCELLED)
                .expiresAt(now.minus(1, ChronoUnit.HOURS))
                .autoRenew(true)
                .build();

        Payment payment = subscriptionService.createAutoRenewPaymentForSubscription(sub);

        assertNull(payment, "No payment should be created for cancelled subscription");
        assertEquals(SubscriptionStatus.CANCELLED, sub.getStatus(), "Status must remain CANCELLED");
        verify(subscriptionRepository, never()).save(sub);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("createAutoRenewPaymentForSubscription handles concurrent constraint collision gracefully")
    void testCreateAutoRenewPaymentConcurrentCollisionHandledGracefully() {
        Instant now = Instant.now();
        Instant currentExpiry = now.minus(5, ChronoUnit.SECONDS);

        Subscription sub = Subscription.builder()
                .id(58L)
                .user(testUser)
                .plan("PRO")
                .price(49.99)
                .billingCycle(BillingCycle.MONTHLY)
                .status(SubscriptionStatus.ACTIVE)
                .expiresAt(currentExpiry)
                .autoRenew(true)
                .build();

        Payment winningPayment = Payment.builder()
                .id(999L)
                .status(PaymentStatus.SUCCESS)
                .amount(49.99)
                .build();

        // First check returns empty, simulating concurrent race condition
        when(paymentRepository.findByIdempotencyKey(anyString()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winningPayment));

        when(paymentRepository.save(any(Payment.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("uk_payment_idempotency_key collision"));

        Payment payment = subscriptionService.createAutoRenewPaymentForSubscription(sub);

        assertNotNull(payment);
        assertEquals(999L, payment.getId());
    }

    @Test
    @DisplayName("cancelSubscription sets status CANCELLED, sets expiry to now, and disables autoRenew")
    void testCancelSubscription() {
        Instant now = Instant.now();
        Subscription sub = Subscription.builder()
                .id(70L)
                .user(testUser)
                .plan("PRO")
                .status(SubscriptionStatus.ACTIVE)
                .expiresAt(now.plus(20, ChronoUnit.DAYS))
                .autoRenew(true)
                .build();

        when(subscriptionRepository.findById(70L)).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));

        SubscriptionResponse response = subscriptionService.cancelSubscription(70L, "user@example.com");

        assertEquals(SubscriptionStatus.CANCELLED, response.getStatus());
        assertEquals(SubscriptionStatus.CANCELLED, response.getEffectiveStatus());
        assertFalse(response.isActive());
        assertFalse(response.isAutoRenew());
    }

    @Test
    @DisplayName("cancelSubscription throws exception when user is unauthorized")
    void testCancelSubscriptionUnauthorized() {
        Subscription sub = Subscription.builder()
                .id(71L)
                .user(testUser) // belongs to user@example.com
                .build();

        when(subscriptionRepository.findById(71L)).thenReturn(Optional.of(sub));

        assertThrows(RuntimeException.class, () ->
                subscriptionService.cancelSubscription(71L, "hacker@example.com")
        );
    }
}
