package com.subscribly.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subscribly.dto.CreatePaymentOrderResponse;
import com.subscribly.dto.WebhookPayload;
import com.subscribly.entity.*;
import com.subscribly.gateway.GatewayOrderResponse;
import com.subscribly.gateway.PaymentGatewayProvider;
import com.subscribly.repository.PaymentRepository;
import com.subscribly.repository.SubscriptionRepository;
import com.subscribly.repository.WebhookEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayWebhookTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentGatewayProvider gatewayProvider;

    @Mock
    private WebhookEventService webhookEventService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PaymentService paymentService;

    private User testUser;
    private Subscription testSubscription;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("subscriber@example.com")
                .fullName("Subscriber One")
                .build();

        Instant start = Instant.parse("2026-10-01T00:00:00Z");
        Instant expiry = Instant.parse("2026-11-01T00:00:00Z");

        testSubscription = Subscription.builder()
                .id(50L)
                .user(testUser)
                .plan("STANDARD")
                .price(49.99)
                .billingCycle(BillingCycle.MONTHLY)
                .status(SubscriptionStatus.ACTIVE)
                .startedAt(start)
                .expiresAt(expiry)
                .startDate(LocalDate.of(2026, 10, 1))
                .endDate(LocalDate.of(2026, 11, 1))
                .autoRenew(true)
                .build();

        testPayment = Payment.builder()
                .id(100L)
                .subscription(testSubscription)
                .user(testUser)
                .idempotencyKey("PAY-REF-XYZ-12345")
                .gatewayOrderId("order_test_987654321")
                .amount(49.99)
                .currency("INR")
                .provider("RAZORPAY")
                .status(PaymentStatus.PENDING)
                .attempts(0)
                .createdAtInstant(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Server-side order creation: initializes payment in PENDING with gateway reference")
    void testCreatePaymentOrderSuccess() {
        when(subscriptionRepository.findById(50L)).thenReturn(Optional.of(testSubscription));
        when(paymentRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(gatewayProvider.createOrder(any(), eq(49.99), eq("INR")))
                .thenReturn(new GatewayOrderResponse("order_rzp_mock123", 49.99, "INR", "created", "rzp_test_key"));
        when(gatewayProvider.getProviderName()).thenReturn("RAZORPAY");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            p.setId(201L);
            return p;
        });

        CreatePaymentOrderResponse response = paymentService.createPaymentOrder(
                50L, "subscriber@example.com", "INR", "my-unique-order-key"
        );

        assertNotNull(response);
        assertEquals("order_rzp_mock123", response.getGatewayOrderId());
        assertEquals("my-unique-order-key", response.getPaymentReference());
        assertEquals(49.99, response.getAmount());
        assertEquals("PENDING", response.getStatus());

        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Server-side order creation: rejects CANCELLED subscription")
    void testCreatePaymentOrderCancelledSubscriptionRejected() {
        testSubscription.setStatus(SubscriptionStatus.CANCELLED);
        when(subscriptionRepository.findById(50L)).thenReturn(Optional.of(testSubscription));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                paymentService.createPaymentOrder(50L, "subscriber@example.com", "INR", "key-cancelled")
        );

        assertTrue(ex.getMessage().contains("CANCELLED"));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Webhook Verification: Valid HMAC signature processes payment and extends subscription exactly once")
    void testValidWebhookSignatureExtendsSubscriptionExactlyOnce() {
        String payload = """
                {
                    "eventId": "evt_capture_111",
                    "eventType": "payment.captured",
                    "orderId": "order_test_987654321",
                    "paymentId": "pay_test_abc123",
                    "amount": 49.99,
                    "currency": "INR"
                }
                """;
        String validSignature = "sha256_mock_valid_signature";
        Instant initialExpiry = testSubscription.getExpiresAt();

        when(gatewayProvider.verifyWebhookSignature(payload, validSignature)).thenReturn(true);
        when(webhookEventService.recordEventIfNew(eq("evt_capture_111"), any(), any())).thenReturn(true);
        when(paymentRepository.findByGatewayOrderId("order_test_987654321")).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        Payment processed = paymentService.processGatewayWebhook(payload, validSignature, null);

        assertNotNull(processed);
        assertEquals(PaymentStatus.SUCCESS, processed.getStatus());
        assertEquals("pay_test_abc123", processed.getGatewayPaymentId());
        assertTrue(testSubscription.getExpiresAt().isAfter(initialExpiry), "Subscription expiry must be extended");

        verify(subscriptionRepository, times(1)).save(testSubscription);
        verify(webhookEventService, times(1)).updateEventStatus("evt_capture_111", "PROCESSED");
        verify(paymentRepository, times(1)).save(testPayment);
    }

    @Test
    @DisplayName("Webhook Verification: Invalid cryptographic signature is strictly rejected with SecurityException")
    void testInvalidWebhookSignatureThrowsSecurityException() {
        String payload = "{\"orderId\":\"order_test_987654321\"}";
        String invalidSignature = "invalid_tampered_signature";

        when(gatewayProvider.verifyWebhookSignature(payload, invalidSignature)).thenReturn(false);

        SecurityException ex = assertThrows(SecurityException.class, () ->
                paymentService.processGatewayWebhook(payload, invalidSignature, null)
        );

        assertTrue(ex.getMessage().contains("Invalid webhook signature"));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(subscriptionRepository, never()).save(any(Subscription.class));
        verify(webhookEventService, never()).recordEventIfNew(any(), any(), any());
    }

    @Test
    @DisplayName("Amount Mismatch: Webhook reporting mismatched amount is rejected, marks payment FAILED without extending subscription")
    void testWebhookAmountMismatchRejected() {
        // Expected amount is 49.99, webhook sends 10.00
        String payload = """
                {
                    "eventId": "evt_mismatch_222",
                    "eventType": "payment.captured",
                    "orderId": "order_test_987654321",
                    "paymentId": "pay_tampered",
                    "amount": 10.00,
                    "currency": "INR"
                }
                """;
        String signature = "sig_valid";
        Instant initialExpiry = testSubscription.getExpiresAt();

        when(gatewayProvider.verifyWebhookSignature(payload, signature)).thenReturn(true);
        when(webhookEventService.recordEventIfNew(eq("evt_mismatch_222"), any(), any())).thenReturn(true);
        when(paymentRepository.findByGatewayOrderId("order_test_987654321")).thenReturn(Optional.of(testPayment));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                paymentService.processGatewayWebhook(payload, signature, null)
        );

        assertTrue(ex.getMessage().contains("amount does not match"));
        assertEquals(PaymentStatus.FAILED, testPayment.getStatus());
        assertTrue(testPayment.getFailureReason().contains("Amount mismatch"));
        assertEquals(initialExpiry, testSubscription.getExpiresAt(), "Entitlement must NOT be granted on amount mismatch");

        verify(subscriptionRepository, never()).save(any(Subscription.class));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(webhookEventService, times(1)).recordDurablePaymentFailure(eq("order_test_987654321"), eq("evt_mismatch_222"), contains("Amount mismatch"));
    }

    @Test
    @DisplayName("Currency Mismatch: Webhook reporting mismatched currency is rejected without extending subscription")
    void testWebhookCurrencyMismatchRejected() {
        // Expected currency is INR, webhook sends USD
        String payload = """
                {
                    "eventId": "evt_curr_mismatch",
                    "eventType": "payment.captured",
                    "orderId": "order_test_987654321",
                    "paymentId": "pay_currency_tamper",
                    "amount": 49.99,
                    "currency": "USD"
                }
                """;
        String signature = "sig_valid";

        when(gatewayProvider.verifyWebhookSignature(payload, signature)).thenReturn(true);
        when(webhookEventService.recordEventIfNew(eq("evt_curr_mismatch"), any(), any())).thenReturn(true);
        when(paymentRepository.findByGatewayOrderId("order_test_987654321")).thenReturn(Optional.of(testPayment));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                paymentService.processGatewayWebhook(payload, signature, null)
        );

        assertTrue(ex.getMessage().contains("Currency does not match"));
        assertEquals(PaymentStatus.FAILED, testPayment.getStatus());
        verify(subscriptionRepository, never()).save(any(Subscription.class));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(webhookEventService, times(1)).recordDurablePaymentFailure(eq("order_test_987654321"), eq("evt_curr_mismatch"), contains("Currency mismatch"));
    }

    @Test
    @DisplayName("Duplicate Delivery: Repeated webhook event is deduplicated via database idempotency without re-extending")
    void testDuplicateWebhookDeliveryIdempotent() {
        String payload = """
                {
                    "eventId": "evt_duplicate_333",
                    "eventType": "payment.captured",
                    "orderId": "order_test_987654321"
                }
                """;
        String signature = "sig_valid";

        when(gatewayProvider.verifyWebhookSignature(payload, signature)).thenReturn(true);
        // Event already exists in DB
        when(webhookEventService.recordEventIfNew(eq("evt_duplicate_333"), any(), any())).thenReturn(false);
        when(paymentRepository.findByGatewayOrderId("order_test_987654321")).thenReturn(Optional.of(testPayment));

        Payment result = paymentService.processGatewayWebhook(payload, signature, null);

        assertNotNull(result);
        assertEquals(testPayment.getId(), result.getId());
        verify(subscriptionRepository, never()).save(any(Subscription.class));
        verify(webhookEventService, never()).updateEventStatus(any(), any());
    }

    @Test
    @DisplayName("Concurrent Duplicate Webhook: DataIntegrityViolationException on isolated sub-transaction gracefully handled")
    void testConcurrentDuplicateWebhookRecoversGracefully() {
        String payload = """
                {
                    "eventId": "evt_concurrent_444",
                    "eventType": "payment.captured",
                    "orderId": "order_test_987654321"
                }
                """;
        String signature = "sig_valid";

        when(gatewayProvider.verifyWebhookSignature(payload, signature)).thenReturn(true);
        // Simulate DB constraint violation when inserting event in REQUIRES_NEW sub-transaction
        when(webhookEventService.recordEventIfNew(eq("evt_concurrent_444"), any(), any()))
                .thenThrow(new DataIntegrityViolationException("uk_webhook_event_id collision"));
        when(paymentRepository.findByGatewayOrderId("order_test_987654321")).thenReturn(Optional.of(testPayment));

        Payment result = paymentService.processGatewayWebhook(payload, signature, null);

        assertNotNull(result);
        assertEquals(testPayment.getId(), result.getId());
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    @DisplayName("Payment Failure Event: Webhook marks payment FAILED and does NOT extend subscription")
    void testPaymentFailureWebhookDoesNotExtendSubscription() {
        String payload = """
                {
                    "eventId": "evt_failed_555",
                    "eventType": "payment.failed",
                    "orderId": "order_test_987654321",
                    "paymentId": "pay_failed_xxx",
                    "failureReason": "Insufficient funds"
                }
                """;
        String signature = "sig_valid";
        Instant initialExpiry = testSubscription.getExpiresAt();

        when(gatewayProvider.verifyWebhookSignature(payload, signature)).thenReturn(true);
        when(webhookEventService.recordEventIfNew(eq("evt_failed_555"), any(), any())).thenReturn(true);
        when(paymentRepository.findByGatewayOrderId("order_test_987654321")).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        Payment result = paymentService.processGatewayWebhook(payload, signature, null);

        assertNotNull(result);
        assertEquals(PaymentStatus.FAILED, result.getStatus());
        assertEquals("Insufficient funds", result.getFailureReason());
        assertEquals(initialExpiry, testSubscription.getExpiresAt(), "Subscription must NOT be extended on failure");

        verify(subscriptionRepository, never()).save(any(Subscription.class));
        verify(paymentRepository, times(1)).save(testPayment);
        verify(webhookEventService, times(1)).updateEventStatus("evt_failed_555", "FAILED");
    }

    @Test
    @DisplayName("Out-of-order / Delayed Events: Delayed payment.failed arriving after verified SUCCESS is safely ignored")
    void testDelayedPaymentFailedEventIgnoredWhenAlreadySuccess() {
        testPayment.setStatus(PaymentStatus.SUCCESS);
        testPayment.setGatewayPaymentId("pay_already_captured");

        String payload = """
                {
                    "eventId": "evt_delayed_fail",
                    "eventType": "payment.failed",
                    "orderId": "order_test_987654321",
                    "failureReason": "Late gateway timeout event"
                }
                """;
        String signature = "sig_valid";

        when(gatewayProvider.verifyWebhookSignature(payload, signature)).thenReturn(true);
        when(webhookEventService.recordEventIfNew(eq("evt_delayed_fail"), any(), any())).thenReturn(true);
        when(paymentRepository.findByGatewayOrderId("order_test_987654321")).thenReturn(Optional.of(testPayment));

        Payment result = paymentService.processGatewayWebhook(payload, signature, null);

        assertEquals(PaymentStatus.SUCCESS, result.getStatus(), "Confirmed SUCCESS status must not be revoked by late failure event");
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    @DisplayName("Unknown Gateway Reference: Webhook for unrecognized order ID throws IllegalArgumentException")
    void testUnknownGatewayOrderIdThrowsException() {
        String payload = """
                {
                    "eventId": "evt_unknown",
                    "eventType": "payment.captured",
                    "orderId": "order_does_not_exist"
                }
                """;
        String signature = "sig_valid";

        when(gatewayProvider.verifyWebhookSignature(payload, signature)).thenReturn(true);
        when(webhookEventService.recordEventIfNew(eq("evt_unknown"), any(), any())).thenReturn(true);
        when(paymentRepository.findByGatewayOrderId("order_does_not_exist")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                paymentService.processGatewayWebhook(payload, signature, null)
        );

        assertTrue(ex.getMessage().contains("Payment order not found"));
    }

    @Test
    @DisplayName("Cancelled Subscription: Webhook attempting to renew CANCELLED subscription is rejected")
    void testWebhookOnCancelledSubscriptionRejected() {
        testSubscription.setStatus(SubscriptionStatus.CANCELLED);

        String payload = """
                {
                    "eventId": "evt_cancelled_sub",
                    "eventType": "payment.captured",
                    "orderId": "order_test_987654321",
                    "amount": 49.99,
                    "currency": "INR"
                }
                """;
        String signature = "sig_valid";

        when(gatewayProvider.verifyWebhookSignature(payload, signature)).thenReturn(true);
        when(webhookEventService.recordEventIfNew(eq("evt_cancelled_sub"), any(), any())).thenReturn(true);
        when(paymentRepository.findByGatewayOrderId("order_test_987654321")).thenReturn(Optional.of(testPayment));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                paymentService.processGatewayWebhook(payload, signature, null)
        );

        assertTrue(ex.getMessage().contains("CANCELLED"));
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    @DisplayName("Transaction Rollback: Database error during subscription save causes transaction exception")
    void testDatabaseFailureRollsBackTransaction() {
        String payload = """
                {
                    "eventId": "evt_db_error",
                    "eventType": "payment.captured",
                    "orderId": "order_test_987654321",
                    "amount": 49.99,
                    "currency": "INR"
                }
                """;
        String signature = "sig_valid";

        when(gatewayProvider.verifyWebhookSignature(payload, signature)).thenReturn(true);
        when(webhookEventService.recordEventIfNew(eq("evt_db_error"), any(), any())).thenReturn(true);
        when(paymentRepository.findByGatewayOrderId("order_test_987654321")).thenReturn(Optional.of(testPayment));
        // Simulate unexpected DB failure when updating subscription
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenThrow(new RuntimeException("Database connection terminated"));

        assertThrows(RuntimeException.class, () ->
                paymentService.processGatewayWebhook(payload, signature, null)
        );

        // Verify payment was NOT saved as SUCCESS because subscription failed
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Missing Amount: Webhook success event without payment amount is rejected, marks payment FAILED without extending subscription")
    void testWebhookSuccessEventWithMissingAmountRejected() {
        String payload = """
                {
                    "eventId": "evt_no_amount",
                    "eventType": "payment.captured",
                    "orderId": "order_test_987654321",
                    "currency": "INR"
                }
                """;
        String signature = "sig_valid";
        Instant initialExpiry = testSubscription.getExpiresAt();

        when(gatewayProvider.verifyWebhookSignature(payload, signature)).thenReturn(true);
        when(webhookEventService.recordEventIfNew(eq("evt_no_amount"), any(), any())).thenReturn(true);
        when(paymentRepository.findByGatewayOrderId("order_test_987654321")).thenReturn(Optional.of(testPayment));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                paymentService.processGatewayWebhook(payload, signature, null)
        );

        assertTrue(ex.getMessage().contains("missing payment amount"));
        assertEquals(PaymentStatus.FAILED, testPayment.getStatus());
        assertEquals(initialExpiry, testSubscription.getExpiresAt(), "Subscription must NOT be extended when amount is missing");
        verify(subscriptionRepository, never()).save(any(Subscription.class));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(webhookEventService, times(1)).recordDurablePaymentFailure(eq("order_test_987654321"), eq("evt_no_amount"), contains("missing payment amount"));
    }

    @Test
    @DisplayName("Create Payment Order: Reusing idempotency key with different request parameters throws IllegalStateException")
    void testCreatePaymentOrderIdempotencyKeyConflictMismatchedParameters() {
        // Setup existing order payment with currency INR and amount 49.99
        when(subscriptionRepository.findById(50L)).thenReturn(Optional.of(testSubscription));
        when(paymentRepository.findByIdempotencyKey("shared-key")).thenReturn(Optional.of(testPayment));

        // Call createPaymentOrder with same key but different currency (USD)
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                paymentService.createPaymentOrder(50L, "subscriber@example.com", "USD", "shared-key")
        );

        assertTrue(ex.getMessage().contains("different request parameters"));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Webhook Verification: Uppercase eventType (e.g. PAYMENT.CAPTURED) is normalized and succeeds")
    void testWebhookWithUppercaseEventTypeSucceeds() {
        String payload = """
                {
                    "eventId": "evt_upper_case",
                    "eventType": "PAYMENT.CAPTURED",
                    "orderId": "order_test_987654321",
                    "paymentId": "pay_test_upper",
                    "amount": 49.99,
                    "currency": "INR"
                }
                """;
        String signature = "sig_valid_upper";

        when(gatewayProvider.verifyWebhookSignature(payload, signature)).thenReturn(true);
        when(webhookEventService.recordEventIfNew(eq("evt_upper_case"), any(), any())).thenReturn(true);
        when(paymentRepository.findByGatewayOrderId("order_test_987654321")).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        Payment processed = paymentService.processGatewayWebhook(payload, signature, null);

        assertNotNull(processed);
        assertEquals(PaymentStatus.SUCCESS, processed.getStatus());
        verify(subscriptionRepository, times(1)).save(testSubscription);
    }

    @Test
    @DisplayName("Webhook Verification: Negative amount is strictly rejected and records durable failure")
    void testWebhookWithNonPositiveAmountRejected() {
        String payload = """
                {
                    "eventId": "evt_neg_amount",
                    "eventType": "payment.captured",
                    "orderId": "order_test_987654321",
                    "paymentId": "pay_test_neg",
                    "amount": -49.99,
                    "currency": "INR"
                }
                """;
        String signature = "sig_valid_neg";

        when(gatewayProvider.verifyWebhookSignature(payload, signature)).thenReturn(true);
        when(webhookEventService.recordEventIfNew(eq("evt_neg_amount"), any(), any())).thenReturn(true);
        when(paymentRepository.findByGatewayOrderId("order_test_987654321")).thenReturn(Optional.of(testPayment));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                paymentService.processGatewayWebhook(payload, signature, null)
        );

        assertTrue(ex.getMessage().contains("non-positive"));
        assertEquals(PaymentStatus.FAILED, testPayment.getStatus());
        verify(subscriptionRepository, never()).save(testSubscription);
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(webhookEventService, times(1)).recordDurablePaymentFailure(eq("order_test_987654321"), eq("evt_neg_amount"), contains("non-positive amount"));
    }

    @Test
    @DisplayName("Webhook Verification: Zero payment amount is strictly rejected and records durable failure")
    void testWebhookWithZeroAmountRejected() {
        String payload = """
                {
                    "eventId": "evt_zero_amount",
                    "eventType": "payment.captured",
                    "orderId": "order_test_987654321",
                    "paymentId": "pay_test_zero",
                    "amount": 0.00,
                    "currency": "INR"
                }
                """;
        String signature = "sig_valid_zero";

        when(gatewayProvider.verifyWebhookSignature(payload, signature)).thenReturn(true);
        when(webhookEventService.recordEventIfNew(eq("evt_zero_amount"), any(), any())).thenReturn(true);
        when(paymentRepository.findByGatewayOrderId("order_test_987654321")).thenReturn(Optional.of(testPayment));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                paymentService.processGatewayWebhook(payload, signature, null)
        );

        assertTrue(ex.getMessage().contains("non-positive"));
        assertEquals(PaymentStatus.FAILED, testPayment.getStatus());
        verify(subscriptionRepository, never()).save(testSubscription);
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(webhookEventService, times(1)).recordDurablePaymentFailure(eq("order_test_987654321"), eq("evt_zero_amount"), contains("non-positive amount"));
    }

    @Test
    @DisplayName("Webhook Verification: Missing gateway order reference in payload is rejected without failure recording")
    void testWebhookWithMissingOrderIdRejected() {
        String payload = """
                {
                    "eventId": "evt_no_order",
                    "eventType": "payment.captured",
                    "paymentId": "pay_test_missing_order",
                    "amount": 49.99,
                    "currency": "INR"
                }
                """;
        String signature = "sig_valid_missing_order";

        when(gatewayProvider.verifyWebhookSignature(payload, signature)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                paymentService.processGatewayWebhook(payload, signature, null)
        );

        assertTrue(ex.getMessage().contains("missing gateway order ID reference"));
        verify(paymentRepository, never()).findByGatewayOrderId(any());
        verify(webhookEventService, never()).recordDurablePaymentFailure(any(), any(), any());
    }

    @Test
    @DisplayName("Transaction Rollback: Database error during payment entity save propagates to roll back subscription")
    void testDatabaseFailureDuringPaymentSaveRollsBackTransaction() {
        String payload = """
                {
                    "eventId": "evt_db_payment_error",
                    "eventType": "payment.captured",
                    "orderId": "order_test_987654321",
                    "amount": 49.99,
                    "currency": "INR"
                }
                """;
        String signature = "sig_valid_pay_err";

        when(gatewayProvider.verifyWebhookSignature(payload, signature)).thenReturn(true);
        when(webhookEventService.recordEventIfNew(eq("evt_db_payment_error"), any(), any())).thenReturn(true);
        when(paymentRepository.findByGatewayOrderId("order_test_987654321")).thenReturn(Optional.of(testPayment));
        // subscription save succeeds
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));
        // payment save fails with DB exception
        when(paymentRepository.save(any(Payment.class)))
                .thenThrow(new DataIntegrityViolationException("Database constraint failure during payment save"));

        assertThrows(DataIntegrityViolationException.class, () ->
                paymentService.processGatewayWebhook(payload, signature, null)
        );

        verify(subscriptionRepository, times(1)).save(testSubscription);
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(webhookEventService, never()).updateEventStatus("evt_db_payment_error", "PROCESSED");
    }

    @Test
    @DisplayName("Webhook Verification: Standard Razorpay payload with order.entity is supported")
    void testWebhookWithOrderEntityPayloadSupported() {
        String payload = """
                {
                    "event": "order.paid",
                    "payload": {
                        "order": {
                            "entity": {
                                "id": "order_test_987654321",
                                "amount": 4999,
                                "currency": "INR",
                                "status": "paid"
                            }
                        }
                    }
                }
                """;
        String signature = "sig_valid_order_entity";

        when(gatewayProvider.verifyWebhookSignature(payload, signature)).thenReturn(true);
        when(webhookEventService.recordEventIfNew(any(), eq("order.paid"), any())).thenReturn(true);
        when(paymentRepository.findByGatewayOrderId("order_test_987654321")).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        Payment processed = paymentService.processGatewayWebhook(payload, signature, "hdr_evt_order_paid");

        assertNotNull(processed);
        assertEquals(PaymentStatus.SUCCESS, processed.getStatus());
        verify(subscriptionRepository, times(1)).save(testSubscription);
    }
}
