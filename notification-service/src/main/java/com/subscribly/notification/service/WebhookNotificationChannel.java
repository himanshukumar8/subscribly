package com.subscribly.notification.service;

import com.subscribly.common.event.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WebhookNotificationChannel implements NotificationChannel {

    @Override
    public String getChannelName() {
        return "WEBHOOK";
    }

    @Override
    public boolean isSupported(DomainEvent event) {
        return false; // Future implementation
    }

    @Override
    public void sendNotification(String recipient, String subject, String content, DomainEvent event) {
        log.debug("[WebhookNotificationChannel] Future Webhook channel: url={}, content={}", recipient, content);
    }
}
