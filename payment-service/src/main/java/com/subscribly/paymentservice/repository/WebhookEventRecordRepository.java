package com.subscribly.paymentservice.repository;

import com.subscribly.paymentservice.entity.WebhookEventRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebhookEventRecordRepository extends JpaRepository<WebhookEventRecord, Long> {

    Optional<WebhookEventRecord> findByEventId(String eventId);

    boolean existsByEventId(String eventId);
}
