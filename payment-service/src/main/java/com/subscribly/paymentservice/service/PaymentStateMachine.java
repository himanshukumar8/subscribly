package com.subscribly.paymentservice.service;

import com.subscribly.paymentservice.entity.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class PaymentStateMachine {

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED_TRANSITIONS = Map.of(
            PaymentStatus.CREATED, EnumSet.of(PaymentStatus.PENDING, PaymentStatus.AUTHORIZED, PaymentStatus.CAPTURED, PaymentStatus.SUCCESS, PaymentStatus.FAILED, PaymentStatus.CANCELLED),
            PaymentStatus.PENDING, EnumSet.of(PaymentStatus.AUTHORIZED, PaymentStatus.CAPTURED, PaymentStatus.SUCCESS, PaymentStatus.FAILED, PaymentStatus.CANCELLED),
            PaymentStatus.AUTHORIZED, EnumSet.of(PaymentStatus.CAPTURED, PaymentStatus.SUCCESS, PaymentStatus.FAILED, PaymentStatus.CANCELLED),
            PaymentStatus.CAPTURED, EnumSet.of(PaymentStatus.SUCCESS, PaymentStatus.REFUNDED),
            PaymentStatus.SUCCESS, EnumSet.of(PaymentStatus.REFUNDED),
            PaymentStatus.FAILED, EnumSet.of(PaymentStatus.PENDING), // Support retry transitions
            PaymentStatus.REFUNDED, EnumSet.noneOf(PaymentStatus.class),
            PaymentStatus.CANCELLED, EnumSet.noneOf(PaymentStatus.class)
    );

    public void validateTransition(PaymentStatus current, PaymentStatus target) {
        if (current == target) {
            return; // Idempotent same-state transition
        }
        Set<PaymentStatus> validNextStates = ALLOWED_TRANSITIONS.getOrDefault(current, EnumSet.noneOf(PaymentStatus.class));
        if (!validNextStates.contains(target)) {
            String errorMsg = String.format("Illegal state transition: Cannot transition payment from %s to %s", current, target);
            log.error("[PaymentStateMachine] {}", errorMsg);
            throw new IllegalStateException(errorMsg);
        }
    }
}
