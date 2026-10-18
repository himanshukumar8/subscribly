package com.subscribly.subscriptionservice.service;

import com.subscribly.common.dto.UserDto;
import com.subscribly.common.event.SubscriptionExpiredEvent;
import com.subscribly.subscriptionservice.client.UserClient;
import com.subscribly.subscriptionservice.dto.SubscriptionRequest;
import com.subscribly.subscriptionservice.entity.BillingCycle;
import com.subscribly.subscriptionservice.entity.Subscription;
import com.subscribly.subscriptionservice.entity.SubscriptionStatus;
import com.subscribly.subscriptionservice.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private UserDto testUserDto;

    @BeforeEach
    void setUp() {
        testUserDto = UserDto.builder()
                .id(1L)
                .email("subscriber@example.com")
                .fullName("Subscriber Test")
                .build();
    }

    @Test
    @DisplayName("createSubscription populates exact startedAt and expiresAt timestamps")
    void testCreateSubscriptionTimestamps() {
        when(userClient.getUserByEmail("subscriber@example.com")).thenReturn(testUserDto);
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> {
            Subscription s = i.getArgument(0);
            s.setId(10L);
            return s;
        });

        SubscriptionRequest req = new SubscriptionRequest();
        req.setPlan("PREMIUM");
        req.setPrice(19.99);
        req.setBillingCycle(BillingCycle.MONTHLY);

        Subscription created = subscriptionService.createSubscription("subscriber@example.com", req);

        assertNotNull(created);
        assertNotNull(created.getStartedAt());
        assertNotNull(created.getExpiresAt());
        assertTrue(created.getExpiresAt().isAfter(created.getStartedAt()));
        assertEquals(SubscriptionStatus.ACTIVE, created.getStatus());
        verify(subscriptionRepository, times(1)).save(any(Subscription.class));
    }

    @Test
    @DisplayName("validateSubscription lazily reconciles status to EXPIRED and publishes event if past timestamp")
    void testValidateSubscriptionExpiredLazyReconciliation() {
        Instant now = Instant.now();
        Subscription expired = Subscription.builder()
                .id(99L)
                .userId(1L)
                .userEmail("subscriber@example.com")
                .plan("PREMIUM")
                .status(SubscriptionStatus.ACTIVE) // DB says ACTIVE
                .startedAt(now.minus(31, ChronoUnit.DAYS))
                .expiresAt(now.minus(1, ChronoUnit.HOURS)) // Expired 1 hour ago
                .build();

        when(subscriptionRepository.findById(99L)).thenReturn(Optional.of(expired));

        Subscription result = subscriptionService.validateSubscription(99L);

        assertEquals(SubscriptionStatus.EXPIRED, result.getStatus());
        verify(subscriptionRepository, times(1)).save(expired);
        verify(kafkaTemplate, times(1)).send(eq("subscription-events"), eq("99"), any(SubscriptionExpiredEvent.class));
    }

    @Test
    @DisplayName("getSubscriptionsByUser reconciles expired subscriptions on query")
    void testGetSubscriptionsByUserReconcilesExpired() {
        Instant now = Instant.now();
        Subscription active = Subscription.builder()
                .id(1L)
                .status(SubscriptionStatus.ACTIVE)
                .expiresAt(now.plus(10, ChronoUnit.DAYS))
                .build();
        Subscription expired = Subscription.builder()
                .id(2L)
                .userId(1L)
                .plan("BASIC")
                .status(SubscriptionStatus.ACTIVE)
                .expiresAt(now.minus(10, ChronoUnit.MINUTES))
                .build();

        when(subscriptionRepository.findByUserEmail("subscriber@example.com")).thenReturn(List.of(active, expired));

        List<Subscription> list = subscriptionService.getSubscriptionsByUser("subscriber@example.com");

        assertEquals(2, list.size());
        assertEquals(SubscriptionStatus.ACTIVE, list.get(0).getStatus());
        assertEquals(SubscriptionStatus.EXPIRED, list.get(1).getStatus());
        verify(subscriptionRepository, times(1)).save(expired);
    }
}
