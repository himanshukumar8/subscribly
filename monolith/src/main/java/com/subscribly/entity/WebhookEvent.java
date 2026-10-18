package com.subscribly.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "webhook_events", uniqueConstraints = {
        @UniqueConstraint(name = "uk_webhook_event_id", columnNames = {"event_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 128)
    private String eventId;

    @Column(name = "event_type", length = 128)
    private String eventType;

    @Builder.Default
    @Column(name = "provider", length = 32)
    private String provider = "RAZORPAY";

    @Column(name = "status", nullable = false, length = 64)
    private String status;

    @Column(name = "payload_hash", length = 64)
    private String payloadHash;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;
}
