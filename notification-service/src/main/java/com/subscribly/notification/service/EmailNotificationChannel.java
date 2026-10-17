package com.subscribly.notification.service;

import com.subscribly.common.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationChannel implements NotificationChannel {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Override
    public String getChannelName() {
        return "EMAIL";
    }

    @Override
    public boolean isSupported(DomainEvent event) {
        return event != null;
    }

    @Override
    public void sendNotification(String recipient, String subject, String content, DomainEvent event) {
        log.info("[EmailNotificationChannel] Preparing email to {}: subject='{}', eventId={}", recipient, subject, event != null ? event.getEventId() : "N/A");
        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(recipient);
                message.setSubject(subject);
                message.setText(content);
                mailSender.send(message);
                log.info("[EmailNotificationChannel] Successfully dispatched email via SMTP to {}", recipient);
            } catch (Exception e) {
                log.warn("[EmailNotificationChannel] SMTP dispatch failed (logging fallback): {}", e.getMessage());
            }
        } else {
            log.info("[EmailNotificationChannel] (Stub/Log Mode) Email to {}: {}", recipient, content);
        }
    }
}
