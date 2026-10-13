package com.eventdriven.order.outbox;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
