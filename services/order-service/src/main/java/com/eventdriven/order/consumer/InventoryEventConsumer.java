package com.eventdriven.order.consumer;

import com.eventdriven.events.KafkaTopics;
import com.eventdriven.events.inventory.InventoryReservedEvent;
import com.eventdriven.events.inventory.ReservationFailedEvent;
import com.eventdriven.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = KafkaTopics.INVENTORY_RESERVED,
            groupId = "${spring.kafka.consumer.group-id:order-service-group}"
    )
    public void handleInventoryReserved(InventoryReservedEvent event) {
        if (event == null || event.getOrderId() == null) {
            log.warn("Received invalid or null InventoryReservedEvent: {}", event);
            return;
        }

        log.info("Received InventoryReservedEvent for orderId: {}", event.getOrderId());
        try {
            orderService.handleInventoryReserved(event.getOrderId());
        } catch (Exception ex) {
            log.error("Error processing InventoryReservedEvent for order {}: {}", event.getOrderId(), ex.getMessage(), ex);
        }
    }

    @KafkaListener(
            topics = KafkaTopics.INVENTORY_RESERVATION_FAILED,
            groupId = "${spring.kafka.consumer.group-id:order-service-group}"
    )
    public void handleReservationFailed(ReservationFailedEvent event) {
        if (event == null || event.getOrderId() == null) {
            log.warn("Received invalid or null ReservationFailedEvent: {}", event);
            return;
        }

        log.info("Received ReservationFailedEvent for orderId: {}, reason: {}", event.getOrderId(), event.getReason());
        try {
            orderService.handleInventoryReservationFailed(event.getOrderId(), event.getReason());
        } catch (Exception ex) {
            log.error("Error processing ReservationFailedEvent for order {}: {}", event.getOrderId(), ex.getMessage(), ex);
        }
    }
}
