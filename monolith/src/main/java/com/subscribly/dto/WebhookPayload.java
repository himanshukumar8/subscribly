package com.subscribly.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookPayload {

    // Direct / Flat fields
    @JsonProperty("id")
    private String id;

    @JsonProperty("eventId")
    private String eventId;

    @JsonProperty("event")
    private String event;

    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("paymentId")
    private String paymentId;

    @JsonProperty("amount")
    private Double amount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("signature")
    private String signature;

    @JsonProperty("failureReason")
    private String failureReason;

    // Razorpay standard nested payload node
    @JsonProperty("payload")
    private JsonNode payloadNode;

    public String resolveEventId(String eventIdHeader) {
        if (eventId != null && !eventId.isBlank()) {
            return eventId;
        }
        if (id != null && !id.isBlank()) {
            return id;
        }
        if (eventIdHeader != null && !eventIdHeader.isBlank()) {
            return eventIdHeader;
        }
        // Fallback to orderId + eventType composite if needed
        String oId = resolveOrderId();
        String eType = resolveEventType();
        if (oId != null && eType != null) {
            return "evt_" + oId + "_" + eType;
        }
        return null;
    }

    public String resolveEventType() {
        if (eventType != null && !eventType.isBlank()) {
            return eventType;
        }
        if (event != null && !event.isBlank()) {
            return event;
        }
        return "unknown";
    }

    public String resolveOrderId() {
        if (orderId != null && !orderId.isBlank()) {
            return orderId;
        }
        if (payloadNode != null && payloadNode.has("payment") && payloadNode.get("payment").has("entity")) {
            JsonNode entity = payloadNode.get("payment").get("entity");
            if (entity.has("order_id")) {
                return entity.get("order_id").asText();
            }
        }
        if (payloadNode != null && payloadNode.has("order") && payloadNode.get("order").has("entity")) {
            JsonNode entity = payloadNode.get("order").get("entity");
            if (entity.has("id")) {
                return entity.get("id").asText();
            }
        }
        return null;
    }

    public String resolvePaymentId() {
        if (paymentId != null && !paymentId.isBlank()) {
            return paymentId;
        }
        if (payloadNode != null && payloadNode.has("payment") && payloadNode.get("payment").has("entity")) {
            JsonNode entity = payloadNode.get("payment").get("entity");
            if (entity.has("id")) {
                return entity.get("id").asText();
            }
        }
        return null;
    }

    public Double resolveAmount() {
        if (amount != null) {
            return amount;
        }
        if (payloadNode != null && payloadNode.has("payment") && payloadNode.get("payment").has("entity")) {
            JsonNode entity = payloadNode.get("payment").get("entity");
            if (entity.has("amount")) {
                return entity.get("amount").asDouble();
            }
        }
        if (payloadNode != null && payloadNode.has("order") && payloadNode.get("order").has("entity")) {
            JsonNode entity = payloadNode.get("order").get("entity");
            if (entity.has("amount")) {
                return entity.get("amount").asDouble();
            }
        }
        return null;
    }

    public String resolveCurrency() {
        if (currency != null && !currency.isBlank()) {
            return currency;
        }
        if (payloadNode != null && payloadNode.has("payment") && payloadNode.get("payment").has("entity")) {
            JsonNode entity = payloadNode.get("payment").get("entity");
            if (entity.has("currency")) {
                return entity.get("currency").asText();
            }
        }
        if (payloadNode != null && payloadNode.has("order") && payloadNode.get("order").has("entity")) {
            JsonNode entity = payloadNode.get("order").get("entity");
            if (entity.has("currency")) {
                return entity.get("currency").asText();
            }
        }
        return "INR";
    }

    public String resolveFailureReason() {
        if (failureReason != null && !failureReason.isBlank()) {
            return failureReason;
        }
        if (payloadNode != null && payloadNode.has("payment") && payloadNode.get("payment").has("entity")) {
            JsonNode entity = payloadNode.get("payment").get("entity");
            if (entity.has("error_description")) {
                return entity.get("error_description").asText();
            }
        }
        if (payloadNode != null && payloadNode.has("order") && payloadNode.get("order").has("entity")) {
            JsonNode entity = payloadNode.get("order").get("entity");
            if (entity.has("error_description")) {
                return entity.get("error_description").asText();
            }
        }
        return "Payment failed via gateway";
    }
}
