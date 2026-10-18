package com.subscribly.paymentservice.service;

import com.subscribly.paymentservice.entity.PaymentAuditLog;
import com.subscribly.paymentservice.entity.PaymentStatus;
import com.subscribly.paymentservice.repository.PaymentAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentAuditService {

    private final PaymentAuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordTransition(Long paymentId, String paymentReference, PaymentStatus fromStatus, PaymentStatus toStatus, String reason, String triggeredBy) {
        log.info("[PaymentAudit] Recording transition for Payment ID: {} [{}] -> [{}], triggeredBy: {}", paymentId, fromStatus, toStatus, triggeredBy);
        PaymentAuditLog logEntry = PaymentAuditLog.builder()
                .paymentId(paymentId)
                .paymentReference(paymentReference)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .reason(reason)
                .triggeredBy(triggeredBy)
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(logEntry);
    }
}
