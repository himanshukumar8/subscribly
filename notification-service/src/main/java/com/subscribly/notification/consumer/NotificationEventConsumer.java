package com.subscribly.notification.consumer;

import com.subscribly.common.event.*;
import com.subscribly.notification.service.NotificationManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationManagerService notificationManager;

    @KafkaListener(topics = "user-events", groupId = "${spring.kafka.consumer.group-id:notification-group}")
    public void consumeUserEvent(DomainEvent event) {
        log.info("[KafkaConsumer] Received user-events topic message: eventId={}, type={}", event.getEventId(), event.getEventType());
        if (event instanceof UserRegisteredEvent uRe) {
            notificationManager.notify(uRe.getEmail(), "Welcome to Subscribly!",
                    "Hello " + uRe.getFullName() + ", your account has been registered successfully.", uRe);
        }
    }

    @KafkaListener(topics = "subscription-events", groupId = "${spring.kafka.consumer.group-id:notification-group}")
    public void consumeSubscriptionEvent(DomainEvent event) {
        log.info("[KafkaConsumer] Received subscription-events topic message: eventId={}, type={}", event.getEventId(), event.getEventType());
        if (event instanceof SubscriptionCreatedEvent sc) {
            notificationManager.notify("user-" + sc.getUserId() + "@subscribly.com", "Subscription Activated",
                    "Your subscription to plan " + sc.getPlanName() + " is now active.", sc);
        } else if (event instanceof SubscriptionRenewedEvent sr) {
            notificationManager.notify("user-" + sr.getUserId() + "@subscribly.com", "Subscription Renewed",
                    "Your subscription to plan " + sr.getPlanName() + " has renewed. Next billing date: " + sr.getNextBillingDate(), sr);
        } else if (event instanceof SubscriptionExpiredEvent se) {
            notificationManager.notify("user-" + se.getUserId() + "@subscribly.com", "Subscription Expired",
                    "Your subscription to plan " + se.getPlanName() + " has expired.", se);
        }
    }

    @KafkaListener(topics = "payment-events", groupId = "${spring.kafka.consumer.group-id:notification-group}")
    public void consumePaymentEvent(DomainEvent event) {
        log.info("[KafkaConsumer] Received payment-events topic message: eventId={}, type={}", event.getEventId(), event.getEventType());
        if (event instanceof PaymentCompletedEvent pc) {
            notificationManager.notify("user-" + pc.getUserId() + "@subscribly.com", "Payment Successful",
                    "We received your payment of $" + pc.getAmount() + " (ref: " + pc.getTransactionReference() + ").", pc);
        } else if (event instanceof PaymentFailedEvent pf) {
            notificationManager.notify("user-" + pf.getUserId() + "@subscribly.com", "Payment Failed",
                    "Your payment of $" + pf.getAmount() + " failed. Reason: " + pf.getFailureReason(), pf);
        }
    }
}
