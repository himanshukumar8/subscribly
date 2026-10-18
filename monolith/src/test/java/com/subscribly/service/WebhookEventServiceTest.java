package com.subscribly.service;

import com.subscribly.entity.WebhookEvent;
import com.subscribly.repository.WebhookEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookEventServiceTest {

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @Mock
    private com.subscribly.repository.PaymentRepository paymentRepository;

    @InjectMocks
    private WebhookEventService webhookEventService;

    @Test
    @DisplayName("recordEventIfNew: returns true and saves event when eventId is new")
    void testRecordEventIfNewSuccess() {
        when(webhookEventRepository.existsByEventId("evt_101")).thenReturn(false);
        when(webhookEventRepository.saveAndFlush(any(WebhookEvent.class))).thenAnswer(i -> i.getArgument(0));

        boolean claimed = webhookEventService.recordEventIfNew("evt_101", "payment.captured", "RAZORPAY");

        assertTrue(claimed);
        ArgumentCaptor<WebhookEvent> captor = ArgumentCaptor.forClass(WebhookEvent.class);
        verify(webhookEventRepository).saveAndFlush(captor.capture());
        WebhookEvent saved = captor.getValue();
        assertEquals("evt_101", saved.getEventId());
        assertEquals("payment.captured", saved.getEventType());
        assertEquals("RAZORPAY", saved.getProvider());
        assertEquals("PROCESSING", saved.getStatus());
        assertNotNull(saved.getReceivedAt());
    }

    @Test
    @DisplayName("recordEventIfNew: returns false when existsByEventId detects pre-existing event")
    void testRecordEventIfNewPreExistingReturnsFalse() {
        when(webhookEventRepository.existsByEventId("evt_already_exists")).thenReturn(true);

        boolean claimed = webhookEventService.recordEventIfNew("evt_already_exists", "payment.captured", "RAZORPAY");

        assertFalse(claimed);
        verify(webhookEventRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("recordEventIfNew: throws DataIntegrityViolationException on race collision to trigger clean sub-tx rollback")
    void testRecordEventIfNewRaceCollisionThrowsDataIntegrityViolation() {
        when(webhookEventRepository.existsByEventId("evt_race")).thenReturn(false);
        when(webhookEventRepository.saveAndFlush(any(WebhookEvent.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint uk_webhook_event_id"));

        assertThrows(DataIntegrityViolationException.class, () ->
                webhookEventService.recordEventIfNew("evt_race", "payment.captured", "RAZORPAY")
        );
    }

    @Test
    @DisplayName("updateEventStatus: successfully updates event status and processedAt")
    void testUpdateEventStatus() {
        WebhookEvent event = WebhookEvent.builder()
                .id(1L)
                .eventId("evt_update_status")
                .status("PROCESSING")
                .build();

        when(webhookEventRepository.findByEventId("evt_update_status")).thenReturn(Optional.of(event));

        webhookEventService.updateEventStatus("evt_update_status", "PROCESSED");

        assertEquals("PROCESSED", event.getStatus());
        assertNotNull(event.getProcessedAt());
        verify(webhookEventRepository).save(event);
    }

    @Test
    @DisplayName("recordDurablePaymentFailure: sets payment status FAILED and updates event status")
    void testRecordDurablePaymentFailureWithExistingPayment() {
        com.subscribly.entity.Payment payment = com.subscribly.entity.Payment.builder()
                .id(101L)
                .gatewayOrderId("order_fail_test")
                .status(com.subscribly.entity.PaymentStatus.PENDING)
                .build();
        WebhookEvent event = WebhookEvent.builder()
                .eventId("evt_fail_test")
                .status("PROCESSING")
                .build();

        when(paymentRepository.findByGatewayOrderId("order_fail_test")).thenReturn(Optional.of(payment));
        when(webhookEventRepository.findByEventId("evt_fail_test")).thenReturn(Optional.of(event));

        webhookEventService.recordDurablePaymentFailure("order_fail_test", "evt_fail_test", "Amount mismatch detected");

        assertEquals(com.subscribly.entity.PaymentStatus.FAILED, payment.getStatus());
        assertEquals("Amount mismatch detected", payment.getFailureReason());
        assertNotNull(payment.getProcessedAtInstant());
        verify(paymentRepository).saveAndFlush(payment);

        assertEquals("FAILED", event.getStatus());
        assertNotNull(event.getProcessedAt());
        verify(webhookEventRepository).saveAndFlush(event);
    }

    @Test
    @DisplayName("recordDurablePaymentFailure: safely handles null or blank gatewayOrderId")
    void testRecordDurablePaymentFailureWithBlankOrderId() {
        webhookEventService.recordDurablePaymentFailure("", "evt_blank", "Some reason");
        verify(paymentRepository, never()).findByGatewayOrderId(any());
        verify(paymentRepository, never()).save(any());
    }
}
