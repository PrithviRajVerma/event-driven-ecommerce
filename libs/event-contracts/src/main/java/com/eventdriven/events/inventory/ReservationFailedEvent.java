package com.eventdriven.events.inventory;

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
public class ReservationFailedEvent {

    public static final String EVENT_TYPE = "INVENTORY_RESERVATION_FAILED";

    private UUID eventId;

    @Builder.Default
    private String eventType = EVENT_TYPE;

    private Instant occurredAt;
    private UUID orderId;
    private String reason;
}
