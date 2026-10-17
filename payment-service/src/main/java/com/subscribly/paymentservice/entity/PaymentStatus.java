package com.subscribly.paymentservice.entity;

public enum PaymentStatus {
    CREATED,
    PENDING,
    AUTHORIZED,
    CAPTURED,
    SUCCESS,
    FAILED,
    REFUNDED,
    CANCELLED
}
