package com.subscribly.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Component
public class RazorpayGatewayProvider implements PaymentGatewayProvider {

    private static final Logger log = LoggerFactory.getLogger(RazorpayGatewayProvider.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    @Value("${razorpay.key-id:rzp_test_mockKeyId}")
    private String keyId;

    @Value("${razorpay.key-secret:rzp_test_mockSecretKey12345}")
    private String keySecret;

    @Value("${razorpay.webhook-secret:rzp_test_mockWebhookSecret12345}")
    private String webhookSecret;

    @Value("${razorpay.api-url:https://api.razorpay.com/v1}")
    private String apiUrl;

    @Value("${razorpay.live-call-enabled:false}")
    private boolean liveCallEnabled = false;

    @Value("${razorpay.connect-timeout-ms:5000}")
    private int connectTimeoutMs = 5000;

    @Value("${razorpay.read-timeout-ms:10000}")
    private int readTimeoutMs = 10000;

    private final ObjectMapper objectMapper;
    private HttpClient httpClient;

    public RazorpayGatewayProvider() {
        this.objectMapper = new ObjectMapper();
        this.liveCallEnabled = false;
    }

    public RazorpayGatewayProvider(String keyId, String keySecret, String webhookSecret) {
        this(keyId, keySecret, webhookSecret, "https://api.razorpay.com/v1", false, null);
    }

    public RazorpayGatewayProvider(
            String keyId,
            String keySecret,
            String webhookSecret,
            String apiUrl,
            boolean liveCallEnabled,
            HttpClient httpClient
    ) {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
        this.apiUrl = apiUrl != null ? apiUrl : "https://api.razorpay.com/v1";
        this.liveCallEnabled = liveCallEnabled;
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        if (liveCallEnabled) {
            validateCredentialsConfigured();
        }
    }

    private synchronized HttpClient getHttpClient() {
        if (this.httpClient == null) {
            this.httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                    .build();
        }
        return this.httpClient;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void setLiveCallEnabled(boolean liveCallEnabled) {
        this.liveCallEnabled = liveCallEnabled;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public boolean isLiveCallEnabled() {
        return liveCallEnabled;
    }

    public boolean isPlaceholder(String val) {
        if (val == null || val.isBlank()) return true;
        String lower = val.trim().toLowerCase(Locale.ROOT);
        return lower.equals("rzp_test_mockkeyid")
                || lower.equals("rzp_test_mocksecretkey12345")
                || lower.equals("rzp_test_mockwebhooksecret12345")
                || lower.contains("your_actual")
                || lower.contains("placeholder");
    }

    @Override
    public GatewayOrderResponse createOrder(String paymentReference, Double amount, String currency) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Payment order amount must be greater than zero");
        }

        if (currency != null && !currency.isBlank() && !currency.trim().matches("^[A-Za-z]{3}$")) {
            throw new IllegalArgumentException("Invalid currency code: " + currency);
        }

        String resolvedCurrency = (currency != null && !currency.isBlank()) ? currency.trim().toUpperCase(Locale.ROOT) : "INR";
        // Razorpay API expects amount in the currency's smallest subunit (paise for INR, cents for USD)
        long amountInMinorUnits = Math.round(amount * 100.0);

        log.info("[RazorpayGateway] Order initiation for reference: {}, amount: {} {} (minor units: {})",
                paymentReference, amount, resolvedCurrency, amountInMinorUnits);

        if (liveCallEnabled) {
            // Real Gateway Mode: strictly executes live API call; fails fast on error, NEVER silently mocks
            return executeLiveOrdersApiCall(paymentReference, amount, amountInMinorUnits, resolvedCurrency);
        } else {
            // Explicit Offline Simulation Mode: applies identical validation and returns deterministic sandbox order
            String sandboxOrderId = "order_sandbox_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            log.info("[RazorpayGateway] Offline simulation order generated: {}", sandboxOrderId);
            return new GatewayOrderResponse(sandboxOrderId, amount, resolvedCurrency, "created", keyId);
        }
    }

    /**
     * Executes real HTTP POST to Razorpay Orders API: https://api.razorpay.com/v1/orders
     * Authenticates with HTTP Basic Auth (key_id : key_secret) and handles timeouts and API errors.
     */
    public GatewayOrderResponse executeLiveOrdersApiCall(
            String paymentReference,
            Double originalAmount,
            long amountInMinorUnits,
            String currency
    ) {
        validateCredentialsConfigured();

        String url = apiUrl + "/orders";
        String authHeader = buildBasicAuthHeader(keyId, keySecret);

        String requestJson = String.format(Locale.ROOT,
                "{\"amount\":%d,\"currency\":\"%s\",\"receipt\":\"%s\",\"notes\":{\"paymentReference\":\"%s\"}}",
                amountInMinorUnits, currency, paymentReference, paymentReference
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(readTimeoutMs))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", authHeader)
                .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
                .build();

        try {
            log.info("[RazorpayGateway] Calling live Razorpay Orders API at {}", url);
            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            String responseBody = response.body();

            if (statusCode >= 200 && statusCode < 300) {
                JsonNode root = objectMapper.readTree(responseBody);
                String orderId = root.path("id").asText();
                String status = root.path("status").asText("created");

                if (orderId == null || orderId.isBlank()) {
                    throw new PaymentGatewayException("Razorpay Orders API succeeded but returned empty order id");
                }

                // Validate returned amount and currency if present in response
                if (root.has("amount") && !root.get("amount").isNull()) {
                    long respAmount = root.get("amount").asLong();
                    if (respAmount != amountInMinorUnits) {
                        throw new PaymentGatewayException("Razorpay Orders API returned mismatched amount: expected " + amountInMinorUnits + ", got " + respAmount);
                    }
                }
                if (root.has("currency") && !root.get("currency").isNull()) {
                    String respCurrency = root.get("currency").asText();
                    if (!respCurrency.equalsIgnoreCase(currency)) {
                        throw new PaymentGatewayException("Razorpay Orders API returned mismatched currency: expected " + currency + ", got " + respCurrency);
                    }
                }

                log.info("[RazorpayGateway] Live order successfully created: orderId={}, status={}", orderId, status);
                return new GatewayOrderResponse(orderId, originalAmount, currency, status, keyId);
            } else {
                String errorDescription = parseErrorDescription(responseBody, statusCode);
                log.error("[RazorpayGateway] Razorpay Orders API error HTTP {}: {}", statusCode, errorDescription);
                throw new PaymentGatewayException("Razorpay Orders API returned error (" + statusCode + "): " + errorDescription);
            }
        } catch (HttpTimeoutException e) {
            log.error("[RazorpayGateway] Connection or read timeout while calling Razorpay Orders API: {}", e.getMessage());
            throw new PaymentGatewayException("Gateway timeout while connecting to Razorpay Orders API", e);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("[RazorpayGateway] Malformed JSON response received from Razorpay Orders API: {}", e.getMessage());
            throw new PaymentGatewayException("Malformed JSON response received from Razorpay Orders API: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("[RazorpayGateway] Network I/O error while calling Razorpay Orders API: {}", e.getMessage());
            throw new PaymentGatewayException("Network error while connecting to Razorpay Orders API: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PaymentGatewayException("Interrupted while calling Razorpay Orders API", e);
        }
    }

    private void validateCredentialsConfigured() {
        if (keyId == null || keyId.isBlank() || isPlaceholder(keyId)) {
            throw new IllegalStateException("Razorpay live gateway mode is enabled (razorpay.live-call-enabled=true), but RAZORPAY_KEY_ID is missing or set to placeholder default. Configure a valid key ID.");
        }
        if (keySecret == null || keySecret.isBlank() || isPlaceholder(keySecret)) {
            throw new IllegalStateException("Razorpay live gateway mode is enabled (razorpay.live-call-enabled=true), but RAZORPAY_KEY_SECRET is missing or set to placeholder default. Configure a valid key secret.");
        }
        if (webhookSecret == null || webhookSecret.isBlank() || isPlaceholder(webhookSecret)) {
            throw new IllegalStateException("Razorpay live gateway mode is enabled (razorpay.live-call-enabled=true), but RAZORPAY_WEBHOOK_SECRET is missing or set to placeholder default. Configure a valid webhook secret.");
        }
    }

    private String buildBasicAuthHeader(String key, String secret) {
        String authString = key + ":" + secret;
        return "Basic " + Base64.getEncoder().encodeToString(authString.getBytes(StandardCharsets.UTF_8));
    }

    private String parseErrorDescription(String responseBody, int statusCode) {
        if (responseBody == null || responseBody.isBlank()) {
            return "HTTP status " + statusCode;
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("error") && root.get("error").has("description")) {
                return root.get("error").get("description").asText();
            }
        } catch (Exception ignored) {
        }
        return "HTTP " + statusCode + ": " + responseBody;
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signatureHeader) {
        if (payload == null || signatureHeader == null || signatureHeader.isBlank()) {
            log.warn("[RazorpayGateway] Webhook signature verification rejected: missing payload or signature header");
            return false;
        }

        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.error("[RazorpayGateway] Webhook signature verification rejected: webhook secret is not configured");
            return false;
        }

        try {
            String expectedSignature = calculateHmacSha256(payload, webhookSecret);
            // Constant-time byte comparison prevents timing side-channel attacks
            byte[] expectedBytes = expectedSignature.getBytes(StandardCharsets.UTF_8);
            byte[] actualBytes = signatureHeader.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
            boolean matches = MessageDigest.isEqual(expectedBytes, actualBytes);

            if (!matches) {
                log.warn("[RazorpayGateway] Webhook cryptographic signature mismatch");
            }
            return matches;
        } catch (Exception e) {
            log.error("[RazorpayGateway] Error during webhook signature verification: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        if (orderId == null || paymentId == null || signature == null || signature.isBlank()) {
            return false;
        }

        if (keySecret == null || keySecret.isBlank()) {
            log.error("[RazorpayGateway] Payment signature verification rejected: key secret is not configured");
            return false;
        }

        try {
            String payload = orderId + "|" + paymentId;
            String expectedSignature = calculateHmacSha256(payload, keySecret);
            byte[] expectedBytes = expectedSignature.getBytes(StandardCharsets.UTF_8);
            byte[] actualBytes = signature.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
            return MessageDigest.isEqual(expectedBytes, actualBytes);
        } catch (Exception e) {
            log.error("[RazorpayGateway] Error during payment signature verification: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "RAZORPAY";
    }

    @Override
    public String getKeyId() {
        return keyId;
    }

    public String calculateHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new SecurityException("Cryptographic signing error", e);
        }
    }
}
