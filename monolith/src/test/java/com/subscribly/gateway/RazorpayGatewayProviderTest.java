package com.subscribly.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RazorpayGatewayProviderTest {

    private RazorpayGatewayProvider provider;

    private static final String KEY_ID = "rzp_test_testKey123";
    private static final String KEY_SECRET = "rzp_test_secretKeyABC";
    private static final String WEBHOOK_SECRET = "rzp_test_webhookSecretXYZ";

    @BeforeEach
    void setUp() {
        provider = new RazorpayGatewayProvider(KEY_ID, KEY_SECRET, WEBHOOK_SECRET);
    }

    @Test
    @DisplayName("Create Order (Offline Simulation): generates valid Razorpay order ID prefixed with 'order_'")
    void testCreateOrderOfflineSimulation() {
        GatewayOrderResponse response = provider.createOrder("REF-12345", 49.99, "INR");

        assertNotNull(response);
        assertNotNull(response.orderId());
        assertTrue(response.orderId().startsWith("order_"));
        assertEquals(49.99, response.amount());
        assertEquals("INR", response.currency());
        assertEquals("created", response.status());
        assertEquals(KEY_ID, response.keyId());
    }

    @Test
    @DisplayName("Create Order: rejects non-positive amounts with IllegalArgumentException")
    void testCreateOrderRejectsNonPositiveAmount() {
        assertThrows(IllegalArgumentException.class, () -> provider.createOrder("REF-1", 0.0, "INR"));
        assertThrows(IllegalArgumentException.class, () -> provider.createOrder("REF-2", -10.0, "INR"));
        assertThrows(IllegalArgumentException.class, () -> provider.createOrder("REF-3", null, "INR"));
    }

    @Test
    @DisplayName("Live HTTP Orders API: Successfully sends HTTP request with Basic Auth, minor units, and parses order ID")
    @SuppressWarnings("unchecked")
    void testLiveOrdersApiCallSuccess() throws Exception {
        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("""
                {
                    "id": "order_live_rzp_99999",
                    "entity": "order",
                    "amount": 4999,
                    "currency": "INR",
                    "status": "created"
                }
                """);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        provider.setHttpClient(mockHttpClient);
        provider.setLiveCallEnabled(true);

        GatewayOrderResponse response = provider.createOrder("REF-LIVE-1", 49.99, "INR");

        assertNotNull(response);
        assertEquals("order_live_rzp_99999", response.orderId());
        assertEquals(49.99, response.amount());
        assertEquals("INR", response.currency());
        assertEquals("created", response.status());
        assertEquals(KEY_ID, response.keyId());

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient).send(requestCaptor.capture(), any());

        HttpRequest captured = requestCaptor.getValue();
        assertEquals("POST", captured.method());
        assertEquals("https://api.razorpay.com/v1/orders", captured.uri().toString());

        String expectedAuth = "Basic " + Base64.getEncoder().encodeToString((KEY_ID + ":" + KEY_SECRET).getBytes());
        assertEquals(expectedAuth, captured.headers().firstValue("Authorization").orElse(null));
        assertEquals("application/json", captured.headers().firstValue("Content-Type").orElse(null));
    }

    @Test
    @DisplayName("Live HTTP Orders API: Handles HTTP 400 Bad Request with parsed Razorpay error description")
    @SuppressWarnings("unchecked")
    void testLiveOrdersApiCallProviderError() throws Exception {
        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);

        when(mockResponse.statusCode()).thenReturn(400);
        when(mockResponse.body()).thenReturn("""
                {
                    "error": {
                        "code": "BAD_REQUEST_ERROR",
                        "description": "Order amount is less than minimum allowable amount"
                    }
                }
                """);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        provider.setHttpClient(mockHttpClient);
        provider.setLiveCallEnabled(true);

        PaymentGatewayException ex = assertThrows(PaymentGatewayException.class, () ->
                provider.createOrder("REF-ERR-1", 0.50, "INR")
        );

        assertTrue(ex.getMessage().contains("Order amount is less than minimum allowable amount"));
    }

    @Test
    @DisplayName("Live HTTP Orders API: Handles timeout with PaymentGatewayException")
    @SuppressWarnings("unchecked")
    void testLiveOrdersApiCallTimeout() throws Exception {
        HttpClient mockHttpClient = mock(HttpClient.class);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new HttpTimeoutException("request timed out"));

        provider.setHttpClient(mockHttpClient);
        provider.setLiveCallEnabled(true);

        PaymentGatewayException ex = assertThrows(PaymentGatewayException.class, () ->
                provider.createOrder("REF-TIMEOUT-1", 49.99, "INR")
        );

        assertTrue(ex.getMessage().contains("timeout"));
    }

    @Test
    @DisplayName("Live HTTP Orders API: Handles network I/O error with PaymentGatewayException")
    @SuppressWarnings("unchecked")
    void testLiveOrdersApiCallNetworkError() throws Exception {
        HttpClient mockHttpClient = mock(HttpClient.class);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Connection refused"));

        provider.setHttpClient(mockHttpClient);
        provider.setLiveCallEnabled(true);

        PaymentGatewayException ex = assertThrows(PaymentGatewayException.class, () ->
                provider.createOrder("REF-IO-1", 49.99, "INR")
        );

        assertTrue(ex.getMessage().contains("Network error"));
    }

    @Test
    @DisplayName("Live HTTP Orders API: Fails safely when credentials are unconfigured or placeholder")
    void testLiveOrdersApiCallFailsWhenCredentialsPlaceholder() {
        RazorpayGatewayProvider placeholderProvider = new RazorpayGatewayProvider("rzp_test_mockKeyId", "rzp_test_mockSecretKey12345", "secret");
        placeholderProvider.setLiveCallEnabled(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                placeholderProvider.createOrder("REF-NO-CREDS", 49.99, "INR")
        );

        assertTrue(ex.getMessage().contains("RAZORPAY_KEY_ID is missing or set to placeholder"));
    }

    @Test
    @DisplayName("Webhook Signature: Valid HMAC-SHA256 signature returns true")
    void testVerifyWebhookSignatureValid() {
        String payload = "{\"event\":\"payment.captured\",\"amount\":4999}";
        String validSignature = provider.calculateHmacSha256(payload, WEBHOOK_SECRET);

        assertTrue(provider.verifyWebhookSignature(payload, validSignature));
    }

    @Test
    @DisplayName("Webhook Signature: Tampered payload or signature returns false")
    void testVerifyWebhookSignatureTampered() {
        String payload = "{\"event\":\"payment.captured\",\"amount\":4999}";
        String validSignature = provider.calculateHmacSha256(payload, WEBHOOK_SECRET);

        // Tamper with payload
        String tamperedPayload = "{\"event\":\"payment.captured\",\"amount\":1000}";
        assertFalse(provider.verifyWebhookSignature(tamperedPayload, validSignature));

        // Tamper with signature
        assertFalse(provider.verifyWebhookSignature(payload, "invalid_signature_hex"));
    }

    @Test
    @DisplayName("Webhook Signature: Null or blank signature returns false")
    void testVerifyWebhookSignatureNullOrBlank() {
        String payload = "{\"event\":\"payment.captured\"}";
        assertFalse(provider.verifyWebhookSignature(payload, null));
        assertFalse(provider.verifyWebhookSignature(payload, ""));
        assertFalse(provider.verifyWebhookSignature(null, "some_sig"));
    }

    @Test
    @DisplayName("Payment Signature: Valid HMAC-SHA256 on order_id|payment_id returns true")
    void testVerifyPaymentSignatureValid() {
        String orderId = "order_12345";
        String paymentId = "pay_67890";
        String payload = orderId + "|" + paymentId;
        String validSignature = provider.calculateHmacSha256(payload, KEY_SECRET);

        assertTrue(provider.verifyPaymentSignature(orderId, paymentId, validSignature));
    }

    @Test
    @DisplayName("Payment Signature: Invalid signature returns false")
    void testVerifyPaymentSignatureInvalid() {
        assertFalse(provider.verifyPaymentSignature("order_12345", "pay_67890", "wrong_signature"));
        assertFalse(provider.verifyPaymentSignature(null, "pay_67890", "sig"));
    }

    @Test
    @DisplayName("Live HTTP Orders API: Handles malformed JSON response with PaymentGatewayException")
    @SuppressWarnings("unchecked")
    void testLiveOrdersApiCallMalformedJson() throws Exception {
        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{ corrupt_json: not-valid }");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        provider.setHttpClient(mockHttpClient);
        provider.setLiveCallEnabled(true);

        PaymentGatewayException ex = assertThrows(PaymentGatewayException.class, () ->
                provider.createOrder("REF-MALFORMED", 49.99, "INR")
        );

        assertTrue(ex.getMessage().contains("Malformed JSON"));
    }

    @Test
    @DisplayName("Live HTTP Orders API: Rejects response with mismatched amount")
    @SuppressWarnings("unchecked")
    void testLiveOrdersApiCallResponseAmountMismatch() throws Exception {
        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("""
                {
                    "id": "order_live_rzp_111",
                    "amount": 1000,
                    "currency": "INR",
                    "status": "created"
                }
                """);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        provider.setHttpClient(mockHttpClient);
        provider.setLiveCallEnabled(true);

        PaymentGatewayException ex = assertThrows(PaymentGatewayException.class, () ->
                provider.createOrder("REF-MISMATCH", 49.99, "INR")
        );

        assertTrue(ex.getMessage().contains("mismatched amount"));
    }

    @Test
    @DisplayName("Live HTTP Orders API: Rejects response with mismatched currency")
    @SuppressWarnings("unchecked")
    void testLiveOrdersApiCallResponseCurrencyMismatch() throws Exception {
        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("""
                {
                    "id": "order_live_rzp_222",
                    "amount": 4999,
                    "currency": "USD",
                    "status": "created"
                }
                """);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        provider.setHttpClient(mockHttpClient);
        provider.setLiveCallEnabled(true);

        PaymentGatewayException ex = assertThrows(PaymentGatewayException.class, () ->
                provider.createOrder("REF-CURR-MISMATCH", 49.99, "INR")
        );

        assertTrue(ex.getMessage().contains("mismatched currency"));
    }

    @Test
    @DisplayName("Webhook & Payment Signature: Null or blank secret returns false safely without NPE")
    void testVerifySignaturesNullOrBlankSecrets() {
        RazorpayGatewayProvider emptySecretProvider = new RazorpayGatewayProvider("rzp_key", null, null);
        assertFalse(emptySecretProvider.verifyWebhookSignature("{}", "sig"));
        assertFalse(emptySecretProvider.verifyPaymentSignature("order_1", "pay_1", "sig"));

        RazorpayGatewayProvider blankSecretProvider = new RazorpayGatewayProvider("rzp_key", "   ", "   ");
        assertFalse(blankSecretProvider.verifyWebhookSignature("{}", "sig"));
        assertFalse(blankSecretProvider.verifyPaymentSignature("order_1", "pay_1", "sig"));
    }

    @Test
    @DisplayName("Create Order: Rejects invalid currency code formatting")
    void testCreateOrderRejectsInvalidCurrency() {
        assertThrows(IllegalArgumentException.class, () ->
                provider.createOrder("REF-CURR", 49.99, "INVALID_CODE")
        );
        assertThrows(IllegalArgumentException.class, () ->
                provider.createOrder("REF-CURR", 49.99, "12")
        );
    }

    @Test
    @DisplayName("Configuration: Explicit offline simulation mode returns simulated order without network call")
    void testExplicitOfflineSimulationMode() {
        RazorpayGatewayProvider offlineProvider = new RazorpayGatewayProvider("rzp_live_abc12345", "secret_live_xyz98765", "webhook_sec");
        offlineProvider.setLiveCallEnabled(false);

        GatewayOrderResponse response = offlineProvider.createOrder("REF-OFFLINE-EXPLICIT", 49.99, "INR");
        assertNotNull(response);
        assertTrue(response.orderId().startsWith("order_sandbox_"));
        assertFalse(offlineProvider.isLiveCallEnabled());
    }

    @Test
    @DisplayName("Configuration: Live mode enabled but missing keyId fails clearly with IllegalStateException")
    void testLiveModeMissingKeyIdThrowsIllegalStateException() {
        RazorpayGatewayProvider noKeyIdProvider = new RazorpayGatewayProvider(null, "secret_live_xyz98765", "webhook_sec");
        noKeyIdProvider.setLiveCallEnabled(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                noKeyIdProvider.createOrder("REF-NO-KEYID", 49.99, "INR")
        );
        assertTrue(ex.getMessage().contains("RAZORPAY_KEY_ID is missing or set to placeholder"));
    }

    @Test
    @DisplayName("Configuration: Live mode enabled but missing keySecret fails clearly with IllegalStateException")
    void testLiveModeMissingKeySecretThrowsIllegalStateException() {
        RazorpayGatewayProvider noSecretProvider = new RazorpayGatewayProvider("rzp_live_realKey123", null, "webhook_sec");
        noSecretProvider.setLiveCallEnabled(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                noSecretProvider.createOrder("REF-NO-SECRET", 49.99, "INR")
        );
        assertTrue(ex.getMessage().contains("RAZORPAY_KEY_SECRET is missing or set to placeholder"));
    }

    @Test
    @DisplayName("Configuration: Live mode enabled but placeholder credentials fails clearly with IllegalStateException")
    void testLiveModePlaceholderCredentialsThrowsIllegalStateException() {
        RazorpayGatewayProvider placeholderProvider = new RazorpayGatewayProvider(
                "rzp_test_mockKeyId", "rzp_test_mockSecretKey12345", "rzp_test_mockWebhookSecret12345"
        );
        placeholderProvider.setLiveCallEnabled(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                placeholderProvider.createOrder("REF-PLACEHOLDER", 49.99, "INR")
        );
        assertTrue(ex.getMessage().contains("RAZORPAY_KEY_ID is missing or set to placeholder"));
    }

    @Test
    @DisplayName("Configuration: Live mode enabled but missing or placeholder webhook secret fails clearly")
    void testLiveModeMissingWebhookSecretThrowsIllegalStateException() {
        RazorpayGatewayProvider noWebhookSecProvider = new RazorpayGatewayProvider("rzp_live_realKey123", "secret_live_realSecret456", null);
        noWebhookSecProvider.setLiveCallEnabled(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                noWebhookSecProvider.createOrder("REF-NO-WEBHOOKSEC", 49.99, "INR")
        );
        assertTrue(ex.getMessage().contains("RAZORPAY_WEBHOOK_SECRET is missing or set to placeholder"));

        RazorpayGatewayProvider placeholderWebhookSecProvider = new RazorpayGatewayProvider("rzp_live_realKey123", "secret_live_realSecret456", "rzp_test_mockWebhookSecret12345");
        placeholderWebhookSecProvider.setLiveCallEnabled(true);

        IllegalStateException ex2 = assertThrows(IllegalStateException.class, () ->
                placeholderWebhookSecProvider.createOrder("REF-PH-WEBHOOKSEC", 49.99, "INR")
        );
        assertTrue(ex2.getMessage().contains("RAZORPAY_WEBHOOK_SECRET is missing or set to placeholder"));
    }

    @Test
    @DisplayName("Configuration: PostConstruct init fails fast when live mode enabled with missing credentials, succeeds in offline mode")
    void testInitLifecycleValidation() {
        RazorpayGatewayProvider offlineProvider = new RazorpayGatewayProvider("rzp_test_mockKeyId", "rzp_test_mockSecretKey12345", "rzp_test_mockWebhookSecret12345");
        offlineProvider.setLiveCallEnabled(false);
        assertDoesNotThrow(offlineProvider::init);

        RazorpayGatewayProvider liveProviderBad = new RazorpayGatewayProvider("rzp_test_mockKeyId", "rzp_test_mockSecretKey12345", "rzp_test_mockWebhookSecret12345");
        liveProviderBad.setLiveCallEnabled(true);
        assertThrows(IllegalStateException.class, liveProviderBad::init);

        RazorpayGatewayProvider liveProviderGood = new RazorpayGatewayProvider("rzp_live_realKey123", "secret_live_realSecret456", "real_webhook_secret_789");
        liveProviderGood.setLiveCallEnabled(true);
        assertDoesNotThrow(liveProviderGood::init);
    }

    @Test
    @DisplayName("Configuration: Live gateway mode NEVER falls back to simulated orders on API failure")
    @SuppressWarnings("unchecked")
    void testLiveGatewayNeverFallsBackToSimulatedOrderOnApiFailure() throws Exception {
        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);

        when(mockResponse.statusCode()).thenReturn(500);
        when(mockResponse.body()).thenReturn("{\"error\":{\"code\":\"GATEWAY_ERROR\",\"description\":\"Internal Server Error on Razorpay\"}}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        RazorpayGatewayProvider liveProvider = new RazorpayGatewayProvider(
                "rzp_live_realKey123", "secret_live_realSecret456", "webhook_sec", "https://api.razorpay.com/v1", true, mockHttpClient
        );

        PaymentGatewayException ex = assertThrows(PaymentGatewayException.class, () ->
                liveProvider.createOrder("REF-FAIL-NO-FALLBACK", 49.99, "INR")
        );

        assertTrue(ex.getMessage().contains("Internal Server Error on Razorpay"));
    }
}
