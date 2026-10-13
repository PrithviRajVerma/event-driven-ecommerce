package com.eventdriven.payment.outbox;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
