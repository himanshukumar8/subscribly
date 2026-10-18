package com.subscribly.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class DomainEvent implements Serializable {
    private String eventId;
    private long timestamp;
    private String eventType;

    public DomainEvent(String eventType) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.eventType = eventType;
    }
}
