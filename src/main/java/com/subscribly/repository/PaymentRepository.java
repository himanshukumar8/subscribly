package com.subscribly.repository;

import com.subscribly.entity.Payment;
import com.subscribly.entity.PaymentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

   
    List<Payment> findBySubscriptionUserEmail(String email);
    List<Payment> findByStatus(PaymentStatus status);

    
    @Query("SELECT p FROM Payment p WHERE (p.status = :retryingStatus AND p.nextRetryAt <= :now) OR p.status = :pendingStatus")
    List<Payment> findPaymentsToProcess(
        @Param("retryingStatus") PaymentStatus retryingStatus,
        @Param("pendingStatus") PaymentStatus pendingStatus,
        @Param("now") LocalDateTime now,
        Pageable pageable
    );
}
