package com.subscribly.notification.service;

import com.subscribly.common.event.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PushNotificationChannel implements NotificationChannel {

    @Override
    public String getChannelName() {
        return "PUSH";
    }

    @Override
    public boolean isSupported(DomainEvent event) {
        return false; // Future implementation
    }

    @Override
    public void sendNotification(String recipient, String subject, String content, DomainEvent event) {
        log.debug("[PushNotificationChannel] Future Push notification: to={}, content={}", recipient, content);
    }
}
