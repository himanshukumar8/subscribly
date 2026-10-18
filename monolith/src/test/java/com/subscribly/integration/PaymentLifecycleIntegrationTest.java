package com.subscribly.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.subscribly.dto.CreatePaymentOrderRequest;
import com.subscribly.dto.CreatePaymentOrderResponse;
import com.subscribly.dto.LoginRequest;
import com.subscribly.dto.RegisterRequest;
import com.subscribly.dto.SubscriptionRequest;
import com.subscribly.dto.SubscriptionResponse;
import com.subscribly.dto.SubscriptionValidationDto;
import com.subscribly.entity.BillingCycle;
import com.subscribly.entity.Payment;
import com.subscribly.entity.PaymentStatus;
import com.subscribly.entity.Subscription;
import com.subscribly.entity.SubscriptionStatus;
import com.subscribly.repository.PaymentRepository;
import com.subscribly.repository.SubscriptionRepository;
import com.subscribly.repository.UserRepository;
import com.subscribly.repository.WebhookEventRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class PaymentLifecycleIntegrationTest {

    private static final String DB_USER = "subscribly_user";
    private static final String DB_PASSWORD = "1";
    private static final String DISPOSABLE_DB_URL = "jdbc:postgresql://localhost:5432/subscribly_disposable_empty";
    private static final String WEBHOOK_SECRET = "rzp_test_mockWebhookSecret12345";
    private static final String TEST_JWT_SECRET = "deterministic-test-jwt-signing-secret-key-min-256-bits-length!";

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Run Flyway migrations on the isolated disposable DB fixture to protect development DB
        Flyway flyway = Flyway.configure()
                .dataSource(DISPOSABLE_DB_URL, DB_USER, DB_PASSWORD)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();
        flyway.clean();
        flyway.migrate();

        registry.add("spring.datasource.url", () -> DISPOSABLE_DB_URL);
        registry.add("spring.datasource.username", () -> DB_USER);
        registry.add("spring.datasource.password", () -> DB_PASSWORD);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("razorpay.webhook-secret", () -> WEBHOOK_SECRET);
        registry.add("razorpay.live-call-enabled", () -> "false");
        registry.add("jwt.secret", () -> TEST_JWT_SECRET);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    @AfterEach
    void cleanUp() {
        // Clean up only our test records in the disposable DB to ensure clean test isolation
        webhookEventRepository.deleteAll();
        paymentRepository.deleteAll();
        subscriptionRepository.deleteAll();
        userRepository.deleteAll();
    }

    private static String computeHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : rawHmac) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC SHA256", e);
        }
    }

    @Test
    @DisplayName("End-to-End Real Payment Lifecycle: Auth -> Sub -> Order -> Webhook -> Idempotency -> Tamper Rejection")
    void testFullPaymentLifecycleWithGenuineData() throws Exception {
        String testEmail = "lifecycle_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String testPassword = "Password123!";

        // 1. User Registration via supported auth flow
        RegisterRequest registerRequest = RegisterRequest.builder()
                .fullName("Integration Tester")
                .email(testEmail)
                .password(testPassword)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(testEmail))
                .andExpect(jsonPath("$.token").isNotEmpty());

        // 2. User Login to obtain valid JWT token
        LoginRequest loginRequest = LoginRequest.builder()
                .email(testEmail)
                .password(testPassword)
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String jwtToken = loginJson.get("token").asText();
        assertNotNull(jwtToken);
        String authHeader = "Bearer " + jwtToken;

        // 3. Create Subscription via actual API using authenticated JWT
        SubscriptionRequest subRequest = SubscriptionRequest.builder()
                .plan("PREMIUM")
                .price(49.99)
                .billingCycle(BillingCycle.MONTHLY)
                .autoRenew(true)
                .build();

        MvcResult subResult = mockMvc.perform(post("/api/v1/subscription/create")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subRequest)))
                .andExpect(status().isOk())
                .andReturn();

        SubscriptionResponse subResponse = objectMapper.readValue(
                subResult.getResponse().getContentAsString(), SubscriptionResponse.class);
        Long subscriptionId = subResponse.getId();
        assertNotNull(subscriptionId);
        Instant initialExpiry = subResponse.getExpiresAt();
        assertNotNull(initialExpiry);

        // Pre-payment validation: Check entitlement via authenticated endpoint
        MvcResult preValidateResult = mockMvc.perform(get("/api/v1/subscription/" + subscriptionId + "/validate")
                        .header("Authorization", authHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        SubscriptionValidationDto preVal = objectMapper.readValue(
                preValidateResult.getResponse().getContentAsString(), SubscriptionValidationDto.class);
        assertTrue(preVal.isActive());
        assertEquals(testEmail, preVal.getUserEmail());

        // 4. Create Payment Order via actual API
        String idempotencyKey = "ORDER-KEY-" + UUID.randomUUID().toString().substring(0, 8);
        CreatePaymentOrderRequest orderRequest = CreatePaymentOrderRequest.builder()
                .subscriptionId(subscriptionId)
                .currency("INR")
                .idempotencyKey(idempotencyKey)
                .build();

        MvcResult orderResult = mockMvc.perform(post("/api/v1/payments/order")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isOk())
                .andReturn();

        CreatePaymentOrderResponse orderResponse = objectMapper.readValue(
                orderResult.getResponse().getContentAsString(), CreatePaymentOrderResponse.class);

        // 5. Retrieve actual persisted payment and expected amount from DB
        String realGatewayOrderId = orderResponse.getGatewayOrderId();
        assertNotNull(realGatewayOrderId, "Gateway order ID must be generated by application");
        assertTrue(realGatewayOrderId.startsWith("order_"), "Gateway order ID must follow order prefix convention");

        Payment persistedPayment = paymentRepository.findByGatewayOrderId(realGatewayOrderId)
                .orElseThrow(() -> new AssertionError("Payment must be persisted with gatewayOrderId"));
        assertEquals(PaymentStatus.PENDING, persistedPayment.getStatus());
        assertEquals(49.99, persistedPayment.getAmount(), 0.001);
        assertEquals("INR", persistedPayment.getCurrency());

        // 6. Build genuine webhook payload accepted by WebhookPayload
        String eventId = "evt_e2e_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        String paymentId = "pay_e2e_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);

        String webhookJson = String.format("""
                {
                    "eventId": "%s",
                    "eventType": "payment.captured",
                    "orderId": "%s",
                    "paymentId": "%s",
                    "amount": %.2f,
                    "currency": "%s"
                }
                """, eventId, realGatewayOrderId, paymentId, persistedPayment.getAmount(), persistedPayment.getCurrency());

        // 7. Sign exact raw UTF-8 JSON bytes with test secret
        String signature = computeHmacSha256(webhookJson, WEBHOOK_SECRET);

        // 8. Process webhook via actual controller
        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("X-Razorpay-Signature", signature)
                        .header("X-Razorpay-Event-Id", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"));

        // 9. Verify payment status and entitlement after processing in DB
        Payment paymentAfter = paymentRepository.findByGatewayOrderId(realGatewayOrderId).orElseThrow();
        assertEquals(PaymentStatus.SUCCESS, paymentAfter.getStatus());
        assertEquals(signature, paymentAfter.getGatewaySignature());
        assertEquals(paymentId, paymentAfter.getGatewayPaymentId());

        Subscription subAfter = subscriptionRepository.findById(subscriptionId).orElseThrow();
        assertEquals(SubscriptionStatus.ACTIVE, subAfter.getStatus());
        assertTrue(subAfter.getExpiresAt().isAfter(initialExpiry), "Subscription expiry must be extended");

        Instant expiryAfterFirstCapture = subAfter.getExpiresAt();

        // Verify entitlement via authenticated validation endpoint
        MvcResult postValidateResult = mockMvc.perform(get("/api/v1/subscription/" + subscriptionId + "/validate")
                        .header("Authorization", authHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        SubscriptionValidationDto postVal = objectMapper.readValue(
                postValidateResult.getResponse().getContentAsString(), SubscriptionValidationDto.class);
        assertEquals(expiryAfterFirstCapture, postVal.getExpiresAt());
        assertTrue(postVal.isActive());

        // 10. Replay the same event -> Idempotency ensures subscription expiry is NOT extended twice
        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("X-Razorpay-Signature", signature)
                        .header("X-Razorpay-Event-Id", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookJson))
                .andExpect(status().isOk());

        Subscription subAfterReplay = subscriptionRepository.findById(subscriptionId).orElseThrow();
        assertEquals(expiryAfterFirstCapture, subAfterReplay.getExpiresAt(),
                "Duplicate webhook delivery must NEVER extend expiry a second time");

        // 11. Tampered / Invalid signature -> Rejected with 400 Bad Request, state unchanged
        String tamperedSignature = "invalid_cryptographic_signature_header_value";
        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("X-Razorpay-Signature", tamperedSignature)
                        .header("X-Razorpay-Event-Id", "evt_tampered")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"));

        Subscription subAfterTampered = subscriptionRepository.findById(subscriptionId).orElseThrow();
        assertEquals(expiryAfterFirstCapture, subAfterTampered.getExpiresAt(),
                "Subscription expiry must remain strictly unchanged on invalid signature");
    }

    @Test
    @DisplayName("Subscription Validation Access Control: 401 unauthenticated, 403 cross-user, 200 owner")
    void testValidateSubscriptionAccessControl() throws Exception {
        // Register and login Alice
        String aliceEmail = "alice_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("Alice", aliceEmail, "Password123!"))))
                .andExpect(status().isOk());

        MvcResult aliceLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(aliceEmail, "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        String aliceToken = objectMapper.readTree(aliceLogin.getResponse().getContentAsString()).get("token").asText();
        String aliceHeader = "Bearer " + aliceToken;

        // Create subscription for Alice
        MvcResult subResult = mockMvc.perform(post("/api/v1/subscription/create")
                        .header("Authorization", aliceHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubscriptionRequest("BASIC", 19.99, BillingCycle.MONTHLY, true, null))))
                .andExpect(status().isOk())
                .andReturn();
        Long subId = objectMapper.readValue(subResult.getResponse().getContentAsString(), SubscriptionResponse.class).getId();

        // 1. Unauthenticated request -> 401 Unauthorized
        mockMvc.perform(get("/api/v1/subscription/" + subId + "/validate")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        // 2. Register and login Mallory
        String malloryEmail = "mallory_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("Mallory", malloryEmail, "Password123!"))))
                .andExpect(status().isOk());

        MvcResult malloryLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(malloryEmail, "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        String malloryToken = objectMapper.readTree(malloryLogin.getResponse().getContentAsString()).get("token").asText();
        String malloryHeader = "Bearer " + malloryToken;

        // Cross-user access attempt by Mallory -> 403 Forbidden
        mockMvc.perform(get("/api/v1/subscription/" + subId + "/validate")
                        .header("Authorization", malloryHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // Authorized access by owner Alice -> 200 OK
        mockMvc.perform(get("/api/v1/subscription/" + subId + "/validate")
                        .header("Authorization", aliceHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionId").value(subId))
                .andExpect(jsonPath("$.userEmail").value(aliceEmail));
    }

    @Test
    @DisplayName("Transaction Isolation: Amount mismatch durably persists FAILED status via REQUIRES_NEW while outer rolls back")
    void testAmountMismatchPersistsFailedStatus() throws Exception {
        String testEmail = "mismatch_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String testPassword = "Password123!";

        // Register user
        RegisterRequest registerRequest = RegisterRequest.builder()
                .fullName("Mismatch Tester")
                .email(testEmail)
                .password(testPassword)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // Login user
        LoginRequest loginRequest = LoginRequest.builder()
                .email(testEmail)
                .password(testPassword)
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String jwtToken = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();
        String authHeader = "Bearer " + jwtToken;

        // Create subscription
        SubscriptionRequest subRequest = SubscriptionRequest.builder()
                .plan("STANDARD")
                .price(29.99)
                .billingCycle(BillingCycle.MONTHLY)
                .build();

        MvcResult subResult = mockMvc.perform(post("/api/v1/subscription/create")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Long subId = objectMapper.readValue(subResult.getResponse().getContentAsString(), SubscriptionResponse.class).getId();
        Instant initialExpiry = subscriptionRepository.findById(subId).orElseThrow().getExpiresAt();

        // Create order
        CreatePaymentOrderRequest orderRequest = CreatePaymentOrderRequest.builder()
                .subscriptionId(subId)
                .currency("INR")
                .build();

        MvcResult orderResult = mockMvc.perform(post("/api/v1/payments/order")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isOk())
                .andReturn();

        CreatePaymentOrderResponse orderResponse = objectMapper.readValue(
                orderResult.getResponse().getContentAsString(), CreatePaymentOrderResponse.class);
        String orderId = orderResponse.getGatewayOrderId();

        // Build webhook with WRONG amount (999.00 instead of 29.99)
        String eventId = "evt_mismatch_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String wrongAmountWebhook = String.format("""
                {
                    "eventId": "%s",
                    "eventType": "payment.captured",
                    "orderId": "%s",
                    "paymentId": "pay_wrong_amt",
                    "amount": 999.00,
                    "currency": "INR"
                }
                """, eventId, orderId);

        String signature = computeHmacSha256(wrongAmountWebhook, WEBHOOK_SECRET);

        // Send webhook with invalid amount
        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("X-Razorpay-Signature", signature)
                        .header("X-Razorpay-Event-Id", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wrongAmountWebhook))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"));

        // Verify in DB: Payment status MUST BE FAILED and failureReason MUST BE PERSISTED via REQUIRES_NEW
        Payment failedPayment = paymentRepository.findByGatewayOrderId(orderId).orElseThrow();
        assertEquals(PaymentStatus.FAILED, failedPayment.getStatus(),
                "Payment must be marked FAILED in DB despite the 400 Bad Request exception");
        assertNotNull(failedPayment.getFailureReason(), "Failure reason must be persisted");
        assertTrue(failedPayment.getFailureReason().contains("Amount mismatch"),
                "Failure reason must describe the amount mismatch");

        // Verify entitlement is strictly NOT granted
        Subscription subAfterFailure = subscriptionRepository.findById(subId).orElseThrow();
        assertEquals(initialExpiry, subAfterFailure.getExpiresAt(),
                "Subscription expiry must NOT be extended on invalid amount");
    }

    @Test
    @DisplayName("Unknown Order ID: Webhook with valid signature but unknown order ID returns 400 without modifying state")
    void testUnknownOrderIdRejectedWithoutCreatingPayment() throws Exception {
        String eventId = "evt_unknown_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String unknownOrderWebhook = String.format("""
                {
                    "eventId": "%s",
                    "eventType": "payment.captured",
                    "orderId": "order_unknown_non_existent_999",
                    "paymentId": "pay_unknown_111",
                    "amount": 49.99,
                    "currency": "INR"
                }
                """, eventId);

        String signature = computeHmacSha256(unknownOrderWebhook, WEBHOOK_SECRET);

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("X-Razorpay-Signature", signature)
                        .header("X-Razorpay-Event-Id", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unknownOrderWebhook))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"));

        Optional<Payment> nonExistent = paymentRepository.findByGatewayOrderId("order_unknown_non_existent_999");
        assertTrue(nonExistent.isEmpty(), "No payment record should be created for unknown gateway order ID");
    }
}
