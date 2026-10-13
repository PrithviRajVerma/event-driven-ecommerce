package com.eventdriven.inventory.outbox;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
