package com.subscribly.repository;

import com.subscribly.entity.Subscription;
import com.subscribly.entity.User;
import com.subscribly.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    // find subscriptions using User object
    List<Subscription> findByUser(User user);

    // find subscriptions using user's email
    List<Subscription> findByUserEmail(String email);

    // find subscriptions due for auto-renew
    List<Subscription> findByStatusAndEndDateLessThanEqual(
            SubscriptionStatus status,
            LocalDate date
    );
}
