package com.subscribly.notification.controller;

import com.subscribly.common.event.DomainEvent;
import com.subscribly.notification.service.NotificationManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/internal/notifications")
@RequiredArgsConstructor
public class InternalNotificationController {

    private final NotificationManagerService notificationManager;

    @PostMapping("/send")
    public ResponseEntity<String> sendInternalNotification(@RequestBody Map<String, String> payload) {
        String recipient = payload.get("recipient");
        String subject = payload.get("subject");
        String content = payload.get("content");
        log.info("[InternalAPI] Service-to-service notification request: recipient={}", recipient);
        notificationManager.notify(recipient, subject, content, new DomainEvent("INTERNAL_NOTIFICATION") {});
        return ResponseEntity.ok("Notification dispatched internally.");
    }
}
