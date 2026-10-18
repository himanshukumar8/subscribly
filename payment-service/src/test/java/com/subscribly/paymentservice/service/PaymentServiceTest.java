package com.subscribly.paymentservice.service;

import com.subscribly.common.dto.SubscriptionDto;
import com.subscribly.common.dto.UserDto;
import com.subscribly.paymentservice.client.SubscriptionClient;
import com.subscribly.paymentservice.client.UserClient;
import com.subscribly.paymentservice.dto.CreateOrderRequest;
import com.subscribly.paymentservice.dto.PaymentResponse;
import com.subscribly.paymentservice.dto.VerifyPaymentRequest;
import com.subscribly.paymentservice.dto.WebhookPayload;
import com.subscribly.paymentservice.entity.Payment;
import com.subscribly.paymentservice.entity.PaymentStatus;
import com.subscribly.paymentservice.entity.WebhookEventRecord;
import com.subscribly.paymentservice.gateway.GatewayOrderResponse;
import com.subscribly.paymentservice.gateway.PaymentGatewayProvider;
import com.subscribly.paymentservice.repository.PaymentAuditLogRepository;
import com.subscribly.paymentservice.repository.PaymentRepository;
import com.subscribly.paymentservice.repository.WebhookEventRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentAuditLogRepository auditLogRepository;

    @Mock
    private WebhookEventRecordRepository webhookEventRecordRepository;

    @Mock
    private PaymentGatewayProvider gatewayProvider;

    @Spy
    private PaymentStateMachine stateMachine = new PaymentStateMachine();

    @Mock
    private PaymentAuditService auditService;

    @Mock
    private UserClient userClient;

    @Mock
    private SubscriptionClient subscriptionClient;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private PaymentService paymentService;

    private Payment testPayment;
    private WebhookPayload testWebhookPayload;

    @BeforeEach
    void setUp() {
        testPayment = Payment.builder()
                .id(1L)
                .paymentReference("PAY-REF-TEST001")
                .gatewayOrderId("order_test_123")
                .subscriptionId(100L)
                .userId(10L)
                .amount(199.99)
                .currency("INR")
                .status(PaymentStatus.CREATED)
                .retryCount(0)
                .build();

        testWebhookPayload = WebhookPayload.builder()
                .eventId("evt_test_webhook_001")
                .eventType("payment.captured")
                .orderId("order_test_123")
                .paymentId("pay_test_999")
                .signature("valid_sig")
                .build();
    }

    @Test
    @DisplayName("Webhook: Duplicate event is ignored idempotently without verifying signature")
    void testProcessWebhookDuplicateEventIdempotent() {
        when(webhookEventRecordRepository.existsByEventId("evt_test_webhook_001")).thenReturn(true);

        boolean result = paymentService.processWebhook("{}", "sig_header", testWebhookPayload);

        assertTrue(result);
        verify(gatewayProvider, never()).verifyWebhookSignature(anyString(), anyString());
        verify(webhookEventRecordRepository, never()).save(any(WebhookEventRecord.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Webhook: Invalid signature throws SecurityException")
    void testProcessWebhookInvalidSignatureThrowsSecurityException() {
        when(webhookEventRecordRepository.existsByEventId("evt_test_webhook_001")).thenReturn(false);
        when(gatewayProvider.verifyWebhookSignature("raw_payload", "invalid_sig")).thenReturn(false);

        SecurityException ex = assertThrows(SecurityException.class, () ->
                paymentService.processWebhook("raw_payload", "invalid_sig", testWebhookPayload));

        assertEquals("Invalid webhook signature header", ex.getMessage());
        verify(webhookEventRecordRepository, never()).save(any(WebhookEventRecord.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Webhook: Valid signature processes event, updates payment to SUCCESS and records audit")
    void testProcessWebhookSuccess() {
        when(webhookEventRecordRepository.existsByEventId("evt_test_webhook_001")).thenReturn(false);
        when(gatewayProvider.verifyWebhookSignature("raw_payload", "valid_sig")).thenReturn(true);
        when(paymentRepository.findByGatewayOrderId("order_test_123")).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = paymentService.processWebhook("raw_payload", "valid_sig", testWebhookPayload);

        assertTrue(result);
        assertEquals(PaymentStatus.SUCCESS, testPayment.getStatus());
        assertEquals("pay_test_999", testPayment.getGatewayPaymentId());
        verify(webhookEventRecordRepository, times(1)).save(any(WebhookEventRecord.class));
        verify(paymentRepository, times(1)).save(testPayment);
        verify(auditService, times(1)).recordTransition(
                eq(1L), eq("PAY-REF-TEST001"), eq(PaymentStatus.CREATED), eq(PaymentStatus.SUCCESS),
                anyString(), eq("WEBHOOK")
        );
    }

    @Test
    @DisplayName("Order: Create order successfully when user and subscription are valid")
    void testCreateOrderSuccess() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .subscriptionId(100L)
                .amount(199.99)
                .currency("INR")
                .build();

        UserDto userDto = UserDto.builder()
                .id(10L)
                .email("alice@example.com")
                .fullName("Alice")
                .roles(java.util.List.of("ROLE_USER"))
                .build();
        SubscriptionDto subscriptionDto = SubscriptionDto.builder()
                .id(100L)
                .userId(10L)
                .plan("PRO")
                .price(199.99)
                .status("ACTIVE")
                .billingCycle("MONTHLY")
                .build();

        when(userClient.getUserByEmail("alice@example.com")).thenReturn(userDto);
        when(subscriptionClient.getSubscriptionById(100L)).thenReturn(subscriptionDto);
        when(gatewayProvider.createOrder(anyString(), eq(199.99), eq("INR")))
                .thenReturn(new GatewayOrderResponse("order_rzp_new", 199.99, "INR", "created"));
        when(gatewayProvider.getProviderName()).thenReturn("Razorpay");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(101L);
            return p;
        });

        PaymentResponse response = paymentService.createOrder("alice@example.com", request);

        assertNotNull(response);
        assertEquals("order_rzp_new", response.getGatewayOrderId());
        assertEquals(PaymentStatus.CREATED, response.getStatus());
        assertEquals(199.99, response.getAmount());
        verify(auditService, times(1)).recordTransition(
                eq(101L), anyString(), isNull(), eq(PaymentStatus.CREATED), anyString(), eq("USER")
        );
    }

    @Test
    @DisplayName("Order: Create order throws exception if user not found")
    void testCreateOrderUserNotFound() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .subscriptionId(100L)
                .amount(199.99)
                .currency("INR")
                .build();

        when(userClient.getUserByEmail("unknown@example.com")).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                paymentService.createOrder("unknown@example.com", request));

        assertTrue(ex.getMessage().contains("User not found"));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Order: Create order throws exception if subscription does not belong to user")
    void testCreateOrderSubscriptionUnauthorized() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .subscriptionId(100L)
                .amount(199.99)
                .currency("INR")
                .build();

        UserDto userDto = UserDto.builder()
                .id(10L)
                .email("alice@example.com")
                .fullName("Alice")
                .roles(java.util.List.of("ROLE_USER"))
                .build();
        SubscriptionDto foreignSub = SubscriptionDto.builder()
                .id(100L)
                .userId(999L)
                .plan("PRO")
                .price(199.99)
                .status("ACTIVE")
                .billingCycle("MONTHLY")
                .build();

        when(userClient.getUserByEmail("alice@example.com")).thenReturn(userDto);
        when(subscriptionClient.getSubscriptionById(100L)).thenReturn(foreignSub);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                paymentService.createOrder("alice@example.com", request));

        assertTrue(ex.getMessage().contains("Unauthorized"));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Verify: Successful signature verification transitions payment to SUCCESS")
    void testVerifyPaymentSuccess() {
        VerifyPaymentRequest request = VerifyPaymentRequest.builder()
                .orderId("order_test_123")
                .paymentId("pay_rzp_456")
                .signature("valid_sig")
                .build();

        when(paymentRepository.findByGatewayOrderId("order_test_123")).thenReturn(Optional.of(testPayment));
        when(gatewayProvider.verifyPaymentSignature("order_test_123", "pay_rzp_456", "valid_sig")).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.verifyPayment("alice@example.com", request);

        assertNotNull(response);
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals("pay_rzp_456", testPayment.getGatewayPaymentId());
        verify(auditService, times(1)).recordTransition(
                eq(1L), eq("PAY-REF-TEST001"), eq(PaymentStatus.CREATED), eq(PaymentStatus.SUCCESS),
                anyString(), eq("USER")
        );
    }

    @Test
    @DisplayName("Verify: Failed signature transitions payment to FAILED and increments retry count")
    void testVerifyPaymentSignatureFailure() {
        VerifyPaymentRequest request = VerifyPaymentRequest.builder()
                .orderId("order_test_123")
                .paymentId("pay_rzp_bad")
                .signature("bad_sig")
                .build();

        when(paymentRepository.findByGatewayOrderId("order_test_123")).thenReturn(Optional.of(testPayment));
        when(gatewayProvider.verifyPaymentSignature("order_test_123", "pay_rzp_bad", "bad_sig")).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.verifyPayment("alice@example.com", request);

        assertNotNull(response);
        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertEquals(1, testPayment.getRetryCount());
        assertNotNull(testPayment.getNextRetryAt());
        verify(auditService, times(1)).recordTransition(
                eq(1L), eq("PAY-REF-TEST001"), eq(PaymentStatus.CREATED), eq(PaymentStatus.FAILED),
                anyString(), eq("USER")
        );
    }

    @Test
    @DisplayName("Verify: Already SUCCESS payment is handled idempotently")
    void testVerifyPaymentAlreadySuccessIdempotent() {
        testPayment.setStatus(PaymentStatus.SUCCESS);
        VerifyPaymentRequest request = VerifyPaymentRequest.builder()
                .orderId("order_test_123")
                .paymentId("pay_rzp_already")
                .signature("any_sig")
                .build();

        when(paymentRepository.findByGatewayOrderId("order_test_123")).thenReturn(Optional.of(testPayment));

        PaymentResponse response = paymentService.verifyPayment("alice@example.com", request);

        assertNotNull(response);
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        verify(gatewayProvider, never()).verifyPaymentSignature(anyString(), anyString(), anyString());
        verify(paymentRepository, never()).save(any(Payment.class));
    }
}
