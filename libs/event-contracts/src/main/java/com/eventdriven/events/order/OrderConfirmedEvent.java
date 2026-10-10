package com.eventdriven.events.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderConfirmedEvent {

    public static final String EVENT_TYPE = "ORDER_CONFIRMED";

    private UUID eventId;

    @Builder.Default
    private String eventType = EVENT_TYPE;

    private Instant occurredAt;
    private UUID orderId;
    private UUID customerId;
}
