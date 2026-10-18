package com.subscribly.service;

import com.subscribly.entity.*;
import com.subscribly.repository.PaymentRepository;
import com.subscribly.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subscribly.gateway.PaymentGatewayProvider;
import com.subscribly.repository.WebhookEventRepository;
import org.mockito.Spy;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentGatewayProvider gatewayProvider;

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @Mock
    private WebhookEventService webhookEventService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PaymentService paymentService;

    private User testUser;
    private Subscription testSubscription;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("alice@example.com")
                .fullName("Alice")
                .build();

        Instant start = Instant.parse("2026-10-01T10:00:00Z");
        Instant expiry = Instant.parse("2026-11-01T10:00:00Z");

        testSubscription = Subscription.builder()
                .id(100L)
                .user(testUser)
                .plan("PRO")
                .price(java.math.BigDecimal.valueOf(49.99))
                .billingCycle(BillingCycle.MONTHLY)
                .status(SubscriptionStatus.ACTIVE)
                .startedAt(start)
                .expiresAt(expiry)
                .startDate(LocalDate.of(2026, 10, 1))
                .endDate(LocalDate.of(2026, 11, 1))
                .autoRenew(true)
                .build();

        paymentService.setSimulationEnabled(true);
    }

    @Test
    @DisplayName("Idempotency: Processing identical key returns existing payment without extending subscription twice")
    void testPaymentIdempotencyReturnsExistingRecordWithoutDoubleExtending() {
        String key = "txn-unique-abc-123";
        Instant initialExpiry = testSubscription.getExpiresAt();

        when(subscriptionRepository.findById(100L)).thenReturn(Optional.of(testSubscription));

        // Setup existing completed payment for this idempotency key
        Payment existingPayment = Payment.builder()
                .id(99L)
                .idempotencyKey(key)
                .subscription(testSubscription)
                .user(testUser)
                .amount(java.math.BigDecimal.valueOf(49.99))
                .status(PaymentStatus.SUCCESS)
                .attempts(1)
                .build();

        when(paymentRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(existingPayment));

        Payment result = paymentService.makePayment(100L, "alice@example.com", key);

        assertNotNull(result);
        assertEquals(99L, result.getId());
        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
        assertEquals(key, result.getIdempotencyKey());

        // Verify subscription was NOT extended a second time
        assertEquals(initialExpiry, testSubscription.getExpiresAt(), "Subscription expiresAt must not change on duplicate idempotency hit");
        verify(subscriptionRepository, never()).save(any(Subscription.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Failed payment schedules retry with backoff and NEVER grants paid entitlement")
    void testFailedPaymentDoesNotGrantEntitlement() {
        String key = "txn-fail-001";
        Duration testBaseBackoff = Duration.ofMinutes(30);
        Instant now = Instant.now();
        Instant initialExpiry = testSubscription.getExpiresAt();

        when(subscriptionRepository.findById(100L)).thenReturn(Optional.of(testSubscription));
        when(paymentRepository.findByIdempotencyKey(key)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            p.setId(201L);
            return p;
        });

        Payment failedPayment = paymentService.processPaymentAttempt(
                100L, "alice@example.com", key, false, "Card declined: insufficient funds", testBaseBackoff
        );

        assertNotNull(failedPayment);
        assertEquals(PaymentStatus.RETRYING, failedPayment.getStatus());
        assertEquals(1, failedPayment.getAttempts());
        assertNotNull(failedPayment.getNextRetryInstant(), "Must schedule nextRetryInstant");
        assertTrue(failedPayment.getNextRetryInstant().isAfter(now), "nextRetryInstant must be in future");
        assertEquals("Card declined: insufficient funds", failedPayment.getFailureReason());

        // CRITICAL: Subscription must NOT have been extended!
        assertEquals(initialExpiry, testSubscription.getExpiresAt(), "Failed payment must NEVER extend subscription expiresAt");
        verify(subscriptionRepository, never()).save(any(Subscription.class));
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Exponential backoff scales geometrically by attempt count (2^(attempt-1))")
    void testExponentialBackoffCalculation() {
        Duration base = Duration.ofHours(1);

        Duration attempt1Backoff = paymentService.calculateBackoff(1, base);
        Duration attempt2Backoff = paymentService.calculateBackoff(2, base);
        Duration attempt3Backoff = paymentService.calculateBackoff(3, base);

        assertEquals(Duration.ofHours(1), attempt1Backoff, "Attempt 1 backoff should be 1 * base (1h)");
        assertEquals(Duration.ofHours(2), attempt2Backoff, "Attempt 2 backoff should be 2 * base (2h)");
        assertEquals(Duration.ofHours(4), attempt3Backoff, "Attempt 3 backoff should be 4 * base (4h)");
    }

    @Test
    @DisplayName("Retry progression: subsequent retry failure increments attempt and updates nextRetryAt")
    void testRetryProgressionWithExponentialBackoff() {
        Duration base = Duration.ofMinutes(10);
        Instant now = Instant.now();

        Payment retryingPayment = Payment.builder()
                .id(301L)
                .subscription(testSubscription)
                .user(testUser)
                .status(PaymentStatus.RETRYING)
                .attempts(1)
                .build();

        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        Payment retried = paymentService.retryPayment(
                retryingPayment, now, false, "Network timeout on gateway", base
        );

        assertEquals(2, retried.getAttempts(), "Attempts must be incremented to 2");
        assertEquals(PaymentStatus.RETRYING, retried.getStatus());
        // Attempt 2 backoff is base * 2 = 20 minutes
        Instant expectedRetry = now.plus(Duration.ofMinutes(20));
        assertEquals(expectedRetry, retried.getNextRetryInstant());
        assertEquals("Network timeout on gateway", retried.getFailureReason());

        // Subscription not extended on failed retry
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    @DisplayName("Retry exhaustion: reaching MAX_RETRIES marks payment FAILED and revokes entitlement")
    void testRetryExhaustionMarksFailedAndRevokesEntitlement() {
        Duration base = Duration.ofMinutes(10);
        Instant now = Instant.now();

        // Expired subscription waiting for retry payment
        testSubscription.setExpiresAt(now.minus(1, ChronoUnit.HOURS));
        testSubscription.setStatus(SubscriptionStatus.ACTIVE);

        Payment retryingPayment = Payment.builder()
                .id(401L)
                .subscription(testSubscription)
                .user(testUser)
                .status(PaymentStatus.RETRYING)
                .attempts(2) // Currently at attempt 2
                .build();

        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        // Attempt 3 fails -> reaches MAX_RETRIES (3)
        Payment exhaustedPayment = paymentService.retryPayment(
                retryingPayment, now, false, "Card permanently blocked", base
        );

        assertEquals(3, exhaustedPayment.getAttempts());
        assertEquals(PaymentStatus.FAILED, exhaustedPayment.getStatus(), "Payment status must transition to FAILED");
        assertNull(exhaustedPayment.getNextRetryInstant(), "Next retry must be cleared on exhaustion");
        assertTrue(exhaustedPayment.getFailureReason().contains("exhausted"), "Failure reason must note retry exhaustion");

        // Subscription status must transition to EXPIRED and entitlement revoked
        assertEquals(SubscriptionStatus.EXPIRED, testSubscription.getStatus());
        assertFalse(testSubscription.hasActiveEntitlement(now), "Entitlement must be revoked upon payment exhaustion");
        verify(subscriptionRepository, times(1)).save(testSubscription);
    }

    @Test
    @DisplayName("Successful payment extends subscription duration and sets status ACTIVE")
    void testSuccessfulPaymentExtendsSubscription() {
        String key = "txn-success-555";
        Instant initialExpiry = testSubscription.getExpiresAt();

        when(subscriptionRepository.findById(100L)).thenReturn(Optional.of(testSubscription));
        when(paymentRepository.findByIdempotencyKey(key)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            p.setId(505L);
            return p;
        });

        Payment payment = paymentService.makePayment(100L, "alice@example.com", key);

        assertNotNull(payment);
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals(1, payment.getAttempts());
        assertNotNull(payment.getProcessedAtInstant());

        // Subscription must have extended by 1 month
        assertTrue(testSubscription.getExpiresAt().isAfter(initialExpiry));
        assertEquals(SubscriptionStatus.ACTIVE, testSubscription.getStatus());
        verify(subscriptionRepository, times(1)).save(testSubscription);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Concurrency: DataIntegrityViolationException on duplicate key gracefully recovers existing record")
    void testConcurrentDuplicateKeyThrowsDataIntegrityAndRecoversExisting() {
        String key = "txn-race-condition";

        when(subscriptionRepository.findById(100L)).thenReturn(Optional.of(testSubscription));
        // First check returned empty (both threads saw no record)
        when(paymentRepository.findByIdempotencyKey(key))
                .thenReturn(Optional.empty()) // First check before insert
                .thenReturn(Optional.of(Payment.builder() // Recovery check after constraint collision
                        .id(777L)
                        .idempotencyKey(key)
                        .status(PaymentStatus.SUCCESS)
                        .build()));

        // Simulate DB unique constraint collision
        when(paymentRepository.save(any(Payment.class)))
                .thenThrow(new DataIntegrityViolationException("uk_payment_idempotency_key violation"));

        Payment recovered = paymentService.makePayment(100L, "alice@example.com", key);

        assertNotNull(recovered);
        assertEquals(777L, recovered.getId());
        assertEquals(key, recovered.getIdempotencyKey());
    }

    @Test
    @DisplayName("Idempotency: Reusing key with different subscription or amount throws IllegalStateException")
    void testIdempotencyPayloadMismatchThrowsIllegalStateException() {
        String key = "txn-payload-mismatch";

        Subscription otherSub = Subscription.builder()
                .id(200L)
                .price(BigDecimal.valueOf(99.0))
                .build();

        // Stored payment for this key belongs to subscription 200 with amount 99.0
        Payment storedPayment = Payment.builder()
                .id(11L)
                .idempotencyKey(key)
                .subscription(otherSub)
                .user(testUser)
                .amount(BigDecimal.valueOf(99.0))
                .status(PaymentStatus.SUCCESS)
                .build();

        when(subscriptionRepository.findById(100L)).thenReturn(Optional.of(testSubscription));
        when(paymentRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(storedPayment));

        // Incoming attempt uses same key for subscription 100 with price 49.99
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                paymentService.makePayment(100L, "alice@example.com", key)
        );

        assertTrue(ex.getMessage().contains("different request parameters"));
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    @DisplayName("Payment attempted on CANCELLED subscription is strictly rejected")
    void testPaymentOnCancelledSubscriptionRejected() {
        testSubscription.setStatus(SubscriptionStatus.CANCELLED);

        when(subscriptionRepository.findById(100L)).thenReturn(Optional.of(testSubscription));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                paymentService.makePayment(100L, "alice@example.com", "key-cancelled-test")
        );

        assertTrue(ex.getMessage().contains("CANCELLED"));
        verify(subscriptionRepository, never()).save(any(Subscription.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Concurrent retry: worker re-reading persistent state skips already SUCCESS payment")
    void testConcurrentRetryWorkerSkipsAlreadySucceededPayment() {
        Payment inMemoryPayment = Payment.builder()
                .id(901L)
                .status(PaymentStatus.RETRYING)
                .attempts(1)
                .subscription(testSubscription)
                .build();

        // Worker 1 has already processed and committed this payment as SUCCESS
        Payment persistentSuccessPayment = Payment.builder()
                .id(901L)
                .status(PaymentStatus.SUCCESS)
                .attempts(2)
                .subscription(testSubscription)
                .build();

        when(paymentRepository.findById(901L)).thenReturn(Optional.of(persistentSuccessPayment));

        Payment result = paymentService.retryPayment(
                inMemoryPayment, Instant.now(), true, null, Duration.ofMinutes(10)
        );

        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
        assertEquals(2, result.getAttempts(), "Attempts should not be incremented again");
        // Verify no extra subscription extension
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    @DisplayName("Gateway Trust Boundary: recordVerifiedGatewayPayment extends subscription and handles duplicate webhook idempotently")
    void testRecordVerifiedGatewayPaymentSuccessAndIdempotency() {
        String ref = "razorpay-pay-99999";
        Instant verifiedAt = Instant.now();
        Instant initialExpiry = testSubscription.getExpiresAt();

        when(subscriptionRepository.findById(100L)).thenReturn(Optional.of(testSubscription));
        when(paymentRepository.findByIdempotencyKey(ref)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            p.setId(808L);
            return p;
        });

        Payment payment = paymentService.recordVerifiedGatewayPayment(
                ref, 100L, "alice@example.com", java.math.BigDecimal.valueOf(49.99), verifiedAt
        );

        assertNotNull(payment);
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals(ref, payment.getIdempotencyKey());
        assertTrue(testSubscription.getExpiresAt().isAfter(initialExpiry));

        // Second call with same gateway reference returns existing payment idempotently
        when(paymentRepository.findByIdempotencyKey(ref)).thenReturn(Optional.of(payment));

        Payment duplicate = paymentService.recordVerifiedGatewayPayment(
                ref, 100L, "alice@example.com", java.math.BigDecimal.valueOf(49.99), verifiedAt
        );

        assertEquals(808L, duplicate.getId());
        verify(subscriptionRepository, times(1)).save(testSubscription); // Only saved once
    }

    @Test
    @DisplayName("Gateway Trust Boundary: payment amount mismatch is rejected")
    void testRecordVerifiedGatewayPaymentMismatchedAmountRejected() {
        when(subscriptionRepository.findById(100L)).thenReturn(Optional.of(testSubscription));

        // Subscription price is 49.99, but gateway reports 10.00
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                paymentService.recordVerifiedGatewayPayment(
                        "ref-tampered-amount", 100L, "alice@example.com", java.math.BigDecimal.valueOf(10.00), Instant.now()
                )
        );

        assertTrue(ex.getMessage().contains("does not match subscription price"));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Authorization: Attempting payment on another user's subscription throws SecurityException")
    void testMakePaymentUnauthorizedUserThrowsSecurityException() {
        when(subscriptionRepository.findById(100L)).thenReturn(Optional.of(testSubscription));

        SecurityException ex = assertThrows(SecurityException.class, () ->
                paymentService.makePayment(100L, "attacker@example.com", "key-unauth")
        );

        assertTrue(ex.getMessage().contains("Unauthorized"));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Simulation Hardening: makePayment fails closed when payment simulation is disabled")
    void testMakePaymentFailsClosedWhenSimulationDisabled() {
        paymentService.setSimulationEnabled(false);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                paymentService.makePayment(100L, "alice@example.com", "key-sim-disabled")
        );

        assertTrue(ex.getMessage().contains("disabled in this environment"));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Authorization: Creating order for another user's subscription throws SecurityException")
    void testCreatePaymentOrderUnauthorizedUserThrowsSecurityException() {
        when(subscriptionRepository.findById(100L)).thenReturn(Optional.of(testSubscription));

        SecurityException ex = assertThrows(SecurityException.class, () ->
                paymentService.createPaymentOrder(100L, "attacker@example.com", "INR", "key-unauth-order")
        );

        assertTrue(ex.getMessage().contains("Unauthorized"));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Authorization: Recording gateway payment for another user's subscription throws SecurityException")
    void testRecordVerifiedGatewayPaymentUnauthorizedThrowsSecurityException() {
        when(subscriptionRepository.findById(100L)).thenReturn(Optional.of(testSubscription));

        SecurityException ex = assertThrows(SecurityException.class, () ->
                paymentService.recordVerifiedGatewayPayment("ref-1", 100L, "attacker@example.com", java.math.BigDecimal.valueOf(49.99), Instant.now())
        );

        assertTrue(ex.getMessage().contains("Unauthorized"));
        verify(paymentRepository, never()).save(any(Payment.class));
    }
}
