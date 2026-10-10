package com.eventdriven.events.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReleasedEvent {

    public static final String EVENT_TYPE = "INVENTORY_RELEASED";

    private UUID eventId;

    @Builder.Default
    private String eventType = EVENT_TYPE;

    private Instant occurredAt;
    private UUID orderId;
    private List<ReleasedItem> releasedItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReleasedItem {
        private UUID productId;
        private int quantity;
    }
}
