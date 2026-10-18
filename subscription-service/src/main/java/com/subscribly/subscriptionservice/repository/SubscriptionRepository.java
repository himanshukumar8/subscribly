package com.subscribly.subscriptionservice.repository;

import com.subscribly.subscriptionservice.entity.Subscription;
import com.subscribly.subscriptionservice.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByUserId(Long userId);

    List<Subscription> findByUserEmail(String userEmail);

    List<Subscription> findByStatusAndEndDateBefore(SubscriptionStatus status, LocalDate date);
}
