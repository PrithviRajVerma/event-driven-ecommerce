package com.eventdriven.inventory.consumer;

import com.eventdriven.events.KafkaTopics;
import com.eventdriven.events.inventory.InventoryReservedEvent;
import com.eventdriven.events.inventory.ReservationFailedEvent;
import com.eventdriven.events.order.OrderCreatedEvent;
import com.eventdriven.inventory.producer.InventoryEventProducer;
import com.eventdriven.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final InventoryService inventoryService;
    private final InventoryEventProducer inventoryEventProducer;

    @KafkaListener(
            topics = KafkaTopics.ORDER_CREATED,
            groupId = "${spring.kafka.consumer.group-id:inventory-service-group}"
    )
    public void handleOrderCreated(OrderCreatedEvent event) {
        if (event == null || event.getOrderId() == null) {
            log.warn("Received invalid or null OrderCreatedEvent: {}", event);
            return;
        }

        log.info("Received OrderCreatedEvent for orderId: {}, itemCount: {}",
                event.getOrderId(),
                event.getItems() != null ? event.getItems().size() : 0);

        try {
            List<InventoryReservedEvent.ReservedItem> reservedItems =
                    inventoryService.reserveOrderInventory(event.getOrderId(), event.getItems());

            InventoryReservedEvent reservedEvent = InventoryReservedEvent.builder()
                    .orderId(event.getOrderId())
                    .customerId(event.getCustomerId())
                    .totalAmount(event.getTotalAmount())
                    .reservedItems(reservedItems)
                    .build();

            inventoryEventProducer.publishInventoryReserved(reservedEvent);
            log.info("Successfully processed order {} and published InventoryReservedEvent", event.getOrderId());
        } catch (Exception ex) {
            log.error("Failed to reserve inventory for order {}: {}", event.getOrderId(), ex.getMessage());

            ReservationFailedEvent failedEvent = ReservationFailedEvent.builder()
                    .orderId(event.getOrderId())
                    .reason(ex.getMessage() != null ? ex.getMessage() : "Failed to reserve inventory")
                    .build();

            inventoryEventProducer.publishReservationFailed(failedEvent);
            log.info("Published ReservationFailedEvent for order {}", event.getOrderId());
        }
    }
}
