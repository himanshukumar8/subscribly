package com.subscribly.repository;

import com.subscribly.entity.Subscription;
import com.subscribly.entity.User;
import com.subscribly.entity.SubscriptionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    
    List<Subscription> findByUser(User user);
    List<Subscription> findByUserEmail(String email);

    
    @Query("SELECT s FROM Subscription s WHERE s.status = :status AND s.endDate <= :now")
    List<Subscription> findDueSubscriptions(
        @Param("status") SubscriptionStatus status, 
        @Param("now") LocalDateTime now, 
        Pageable pageable
    );
}
