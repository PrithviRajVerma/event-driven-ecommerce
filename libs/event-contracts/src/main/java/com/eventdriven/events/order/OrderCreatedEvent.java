package com.eventdriven.events.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {

    public static final String EVENT_TYPE = "ORDER_CREATED";

    private UUID eventId;

    @Builder.Default
    private String eventType = EVENT_TYPE;

    private Instant occurredAt;
    private UUID orderId;
    private UUID customerId;
    private List<OrderItem> items;
    private BigDecimal totalAmount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private UUID productId;
        private int quantity;
        private BigDecimal unitPrice;
    }
}
