package com.subscribly.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subscribly.entity.*;
import com.subscribly.gateway.PaymentGatewayProvider;
import com.subscribly.repository.PaymentRepository;
import com.subscribly.repository.SubscriptionRepository;
import com.subscribly.repository.UserRepository;
import com.subscribly.scheduler.SubscriptionScheduler;
import com.subscribly.service.PaymentService;
import com.subscribly.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentSimulationSecurityTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentGatewayProvider gatewayProvider;

    private PaymentService paymentService;
    private SubscriptionService subscriptionService;
    private SubscriptionScheduler scheduler;

    private User testUser;
    private Subscription testSubscription;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository,
                subscriptionRepository,
                gatewayProvider,
                null,
                new ObjectMapper()
        );
        subscriptionService = new SubscriptionService(
                userRepository,
                subscriptionRepository,
                paymentRepository
        );
        scheduler = new SubscriptionScheduler(
                subscriptionRepository,
                subscriptionService,
                paymentRepository,
                paymentService
        );

        testUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .fullName("Test User")
                .build();

        Instant start = Instant.now().minus(30, ChronoUnit.DAYS);
        Instant expiry = Instant.now().minus(5, ChronoUnit.MINUTES);

        testSubscription = Subscription.builder()
                .id(100L)
                .user(testUser)
                .plan("PRO")
                .price(new BigDecimal("49.99"))
                .billingCycle(BillingCycle.MONTHLY)
                .status(SubscriptionStatus.ACTIVE)
                .startedAt(start)
                .expiresAt(expiry)
                .autoRenew(true)
                .build();
    }

    @Test
    @DisplayName("Simulation Off: Direct manual payment (/pay) fails closed with IllegalStateException")
    void testManualPaymentFailsClosedWhenSimulationDisabled() {
        paymentService.setSimulationEnabled(false);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                paymentService.makePayment(100L, "user@example.com", "key-1")
        );

        assertTrue(ex.getMessage().contains("disabled in this environment"));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Simulation Off: Scheduler auto-renew sweep does NOT grant entitlement or create false ledger success")
    void testSchedulerAutoRenewFailsClosedWhenSimulationDisabled() {
        scheduler.setSimulationEnabled(false);
        subscriptionService.setSimulationEnabled(false);

        when(subscriptionRepository.findDueSubscriptions(any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(testSubscription));
        when(paymentRepository.findPaymentsToProcessInstant(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());

        scheduler.processContinuousAutoRenewals();

        // Must NOT save any payment
        verify(paymentRepository, never()).save(any(Payment.class));
        // Sub must transition to EXPIRED, NOT ACTIVE
        assertEquals(SubscriptionStatus.EXPIRED, testSubscription.getStatus());
        verify(subscriptionRepository, atLeastOnce()).save(testSubscription);
    }

    @Test
    @DisplayName("Simulation Off: Payment retry does NOT simulate success; fails closed and exhausts to FAILED")
    void testPaymentRetryFailsClosedWithoutExtendingEntitlement() {
        paymentService.setSimulationEnabled(false);

        Payment retryingPayment = Payment.builder()
                .id(501L)
                .subscription(testSubscription)
                .user(testUser)
                .amount(new BigDecimal("49.99"))
                .status(PaymentStatus.RETRYING)
                .attempts(PaymentService.MAX_RETRIES - 1) // 2 attempts already
                .build();

        when(paymentRepository.findById(501L)).thenReturn(Optional.of(retryingPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        Instant preRetryExpiry = testSubscription.getExpiresAt();

        Payment result = paymentService.retryPayment(
                retryingPayment,
                Instant.now(),
                false, // simulateSuccess is false!
                "Live gateway unavailable",
                Duration.ofMinutes(1)
        );

        // Verification: Payment must be FAILED, attempts = MAX_RETRIES (3)
        assertEquals(PaymentStatus.FAILED, result.getStatus());
        assertEquals(PaymentService.MAX_RETRIES, result.getAttempts());
        assertTrue(result.getFailureReason().contains("Retry exhausted"));

        // Critical Security Invariant: Expiry timestamp must NOT have been extended!
        assertEquals(preRetryExpiry, testSubscription.getExpiresAt());
        assertEquals(SubscriptionStatus.EXPIRED, testSubscription.getStatus());
    }

    @Test
    @DisplayName("Simulation On: Explicit opt-in in dev/test environment allows simulated auto-renewal")
    void testExplicitSimulationOptInAllowsAutoRenew() {
        scheduler.setSimulationEnabled(true);
        subscriptionService.setSimulationEnabled(true);

        when(subscriptionRepository.findDueSubscriptions(any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(testSubscription));
        when(paymentRepository.findPaymentsToProcessInstant(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            p.setId(901L);
            return p;
        });

        scheduler.processContinuousAutoRenewals();

        verify(paymentRepository, times(1)).save(any(Payment.class));
        assertEquals(SubscriptionStatus.ACTIVE, testSubscription.getStatus());
        assertTrue(testSubscription.getExpiresAt().isAfter(Instant.now()));
    }
}
