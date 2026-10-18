package com.subscribly.paymentservice.repository;

import com.subscribly.paymentservice.entity.PaymentAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentAuditLogRepository extends JpaRepository<PaymentAuditLog, Long> {

    List<PaymentAuditLog> findByPaymentIdOrderByTimestampAsc(Long paymentId);

    List<PaymentAuditLog> findByPaymentReferenceOrderByTimestampAsc(String paymentReference);
}
