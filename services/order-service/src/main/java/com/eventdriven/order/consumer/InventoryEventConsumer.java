package com.eventdriven.order.consumer;

import com.eventdriven.events.KafkaTopics;
import com.eventdriven.events.inventory.InventoryReservedEvent;
import com.eventdriven.events.inventory.ReservationFailedEvent;
import com.eventdriven.order.idempotency.IdempotencyService;
import com.eventdriven.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventConsumer {

    private static final String CONSUMER_GROUP = "order-service-group";

    private final OrderService orderService;
    private final IdempotencyService idempotencyService;

    @KafkaListener(
            topics = KafkaTopics.INVENTORY_RESERVED,
            groupId = "${spring.kafka.consumer.group-id:" + CONSUMER_GROUP + "}"
    )
    public void handleInventoryReserved(InventoryReservedEvent event) {
        if (event == null || event.getOrderId() == null) {
            log.warn("Received invalid or null InventoryReservedEvent: {}", event);
            return;
        }

        if (event.getEventId() != null && idempotencyService.isAlreadyProcessed(event.getEventId())) {
            log.warn("Duplicate event {} detected in InventoryReservedEvent. Skipping.", event.getEventId());
            return;
        }

        log.info("Received InventoryReservedEvent for orderId: {}", event.getOrderId());
        try {
            orderService.handleInventoryReserved(event.getOrderId());
            if (event.getEventId() != null) {
                idempotencyService.markAsProcessed(event.getEventId(), event.getEventType(), CONSUMER_GROUP);
            }
        } catch (Exception ex) {
            log.error("Error processing InventoryReservedEvent for order {}: {}", event.getOrderId(), ex.getMessage(), ex);
        }
    }

    @KafkaListener(
            topics = KafkaTopics.INVENTORY_RESERVATION_FAILED,
            groupId = "${spring.kafka.consumer.group-id:" + CONSUMER_GROUP + "}"
    )
    public void handleReservationFailed(ReservationFailedEvent event) {
        if (event == null || event.getOrderId() == null) {
            log.warn("Received invalid or null ReservationFailedEvent: {}", event);
            return;
        }

        if (event.getEventId() != null && idempotencyService.isAlreadyProcessed(event.getEventId())) {
            log.warn("Duplicate event {} detected in ReservationFailedEvent. Skipping.", event.getEventId());
            return;
        }

        log.info("Received ReservationFailedEvent for orderId: {}, reason: {}", event.getOrderId(), event.getReason());
        try {
            orderService.handleInventoryReservationFailed(event.getOrderId(), event.getReason());
            if (event.getEventId() != null) {
                idempotencyService.markAsProcessed(event.getEventId(), event.getEventType(), CONSUMER_GROUP);
            }
        } catch (Exception ex) {
            log.error("Error processing ReservationFailedEvent for order {}: {}", event.getOrderId(), ex.getMessage(), ex);
        }
    }
}
