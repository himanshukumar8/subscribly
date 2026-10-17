package com.subscribly.paymentservice.controller;

import com.subscribly.paymentservice.dto.PaymentResponse;
import com.subscribly.paymentservice.entity.PaymentAuditLog;
import com.subscribly.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentService paymentService;

    @GetMapping("/all")
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @GetMapping("/{id}/audit-logs")
    public ResponseEntity<List<PaymentAuditLog>> getPaymentAuditLogs(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getAuditLogs(id));
    }
}
