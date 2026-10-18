package com.subscribly.notification.service;

import com.subscribly.common.event.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsNotificationChannel implements NotificationChannel {

    @Override
    public String getChannelName() {
        return "SMS";
    }

    @Override
    public boolean isSupported(DomainEvent event) {
        return false; // Future implementation
    }

    @Override
    public void sendNotification(String recipient, String subject, String content, DomainEvent event) {
        log.debug("[SmsNotificationChannel] Future SMS channel: to={}, content={}", recipient, content);
    }
}
