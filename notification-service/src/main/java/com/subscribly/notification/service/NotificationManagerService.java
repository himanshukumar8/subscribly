package com.subscribly.notification.service;

import com.subscribly.common.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationManagerService {

    private final List<NotificationChannel> channels;

    public void notify(String recipient, String subject, String content, DomainEvent event) {
        log.info("[NotificationManager] Processing notification via {} available channels", channels.size());
        for (NotificationChannel channel : channels) {
            if (channel.isSupported(event)) {
                log.info("[NotificationManager] Dispatching via channel: {}", channel.getChannelName());
                channel.sendNotification(recipient, subject, content, event);
            }
        }
    }
}
