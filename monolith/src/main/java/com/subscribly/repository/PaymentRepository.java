package com.subscribly.repository;

import com.subscribly.entity.Payment;
import com.subscribly.entity.PaymentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findBySubscriptionUserEmail(String email);

    List<Payment> findByStatus(PaymentStatus status);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);

    @Query("SELECT p FROM Payment p WHERE (p.status = :retryingStatus AND p.nextRetryAt <= :now) OR p.status = :pendingStatus")
    List<Payment> findPaymentsToProcess(
        @Param("retryingStatus") PaymentStatus retryingStatus,
        @Param("pendingStatus") PaymentStatus pendingStatus,
        @Param("now") LocalDate now,
        Pageable pageable
    );

    @Query("SELECT p FROM Payment p WHERE (p.status = :retryingStatus AND ((p.nextRetryInstant IS NOT NULL AND p.nextRetryInstant <= :nowInstant) OR (p.nextRetryInstant IS NULL AND p.nextRetryAt <= :nowDate))) OR p.status = :pendingStatus")
    List<Payment> findPaymentsToProcessInstant(
        @Param("retryingStatus") PaymentStatus retryingStatus,
        @Param("pendingStatus") PaymentStatus pendingStatus,
        @Param("nowInstant") Instant nowInstant,
        @Param("nowDate") LocalDate nowDate,
        Pageable pageable
    );
}
