package com.subscribly.notification.service;

import com.subscribly.common.event.DomainEvent;

public interface NotificationChannel {
    String getChannelName();
    boolean isSupported(DomainEvent event);
    void sendNotification(String recipient, String subject, String content, DomainEvent event);
}
