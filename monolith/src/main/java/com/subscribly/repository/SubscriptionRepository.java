package com.subscribly.repository;

import com.subscribly.entity.Subscription;
import com.subscribly.entity.User;
import com.subscribly.entity.SubscriptionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByUser(User user);
    List<Subscription> findByUserEmail(String email);

    @Query("SELECT s FROM Subscription s WHERE s.status = :status AND " +
           "((s.expiresAt IS NOT NULL AND s.expiresAt <= :now) OR " +
           "(s.expiresAt IS NULL AND s.endDate <= :today))")
    List<Subscription> findDueSubscriptions(
        @Param("status") SubscriptionStatus status, 
        @Param("now") Instant now, 
        @Param("today") LocalDate today, 
        Pageable pageable
    );

    @Query("SELECT s FROM Subscription s WHERE s.status = :status AND s.endDate <= :now")
    List<Subscription> findDueSubscriptions(
        @Param("status") SubscriptionStatus status, 
        @Param("now") LocalDate now, 
        Pageable pageable
    );
}

