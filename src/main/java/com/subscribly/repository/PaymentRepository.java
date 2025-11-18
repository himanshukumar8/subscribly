package com.subscribly.repository;

import com.subscribly.entity.Payment;
import com.subscribly.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findBySubscriptionUserEmail(String email);

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByStatusAndNextRetryAtBefore(PaymentStatus status, LocalDate date);
}
