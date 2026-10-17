package com.subscribly.paymentservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyPaymentRequest {
    @NotBlank(message = "Order ID is required")
    private String orderId;
    @NotBlank(message = "Payment ID is required")
    private String paymentId;
    @NotBlank(message = "Signature is required")
    private String signature;
}
