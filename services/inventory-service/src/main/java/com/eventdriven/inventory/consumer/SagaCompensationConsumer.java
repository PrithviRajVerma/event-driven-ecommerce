package com.eventdriven.inventory.consumer;

import com.eventdriven.events.KafkaTopics;
import com.eventdriven.events.inventory.InventoryReleasedEvent;
import com.eventdriven.events.order.OrderCancelledEvent;
import com.eventdriven.events.payment.PaymentFailedEvent;
import com.eventdriven.inventory.idempotency.IdempotencyService;
import com.eventdriven.inventory.outbox.OutboxService;
import com.eventdriven.inventory.producer.InventoryEventProducer;
import com.eventdriven.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SagaCompensationConsumer {

    private static final String CONSUMER_GROUP = "inventory-service-compensation-group";

    private final InventoryService inventoryService;
    private final InventoryEventProducer inventoryEventProducer;
    private final IdempotencyService idempotencyService;
    private final OutboxService outboxService;

    @KafkaListener(
            topics = KafkaTopics.PAYMENT_FAILED,
            groupId = "${spring.kafka.consumer.group-id:" + CONSUMER_GROUP + "}"
    )
    public void handlePaymentFailed(PaymentFailedEvent event) {
        if (event == null || event.getOrderId() == null) {
            log.warn("Received invalid or null PaymentFailedEvent in compensation consumer: {}", event);
            return;
        }

        if (event.getEventId() != null && idempotencyService.isAlreadyProcessed(event.getEventId())) {
            log.warn("Duplicate PaymentFailedEvent {} detected. Skipping compensation.", event.getEventId());
            return;
        }

        log.info("Received PaymentFailedEvent for order {}. Triggering inventory release compensation.", event.getOrderId());
        executeCompensation(event.getOrderId(), event.getReason() != null ? event.getReason() : "Payment failed", event.getEventId(), event.getEventType());
    }

    @KafkaListener(
            topics = KafkaTopics.ORDER_CANCELLED,
            groupId = "${spring.kafka.consumer.group-id:" + CONSUMER_GROUP + "}"
    )
    public void handleOrderCancelled(OrderCancelledEvent event) {
        if (event == null || event.getOrderId() == null) {
            log.warn("Received invalid or null OrderCancelledEvent in compensation consumer: {}", event);
            return;
        }

        if (event.getEventId() != null && idempotencyService.isAlreadyProcessed(event.getEventId())) {
            log.warn("Duplicate OrderCancelledEvent {} detected. Skipping compensation.", event.getEventId());
            return;
        }

        log.info("Received OrderCancelledEvent for order {}. Triggering inventory release compensation.", event.getOrderId());
        executeCompensation(event.getOrderId(), event.getReason() != null ? event.getReason() : "Order cancelled", event.getEventId(), event.getEventType());
    }

    private void executeCompensation(UUID orderId, String reason, UUID eventId, String eventType) {
        try {
            List<InventoryReleasedEvent.ReleasedItem> releasedItems =
                    inventoryService.releaseOrderInventory(orderId, reason);

            if (!releasedItems.isEmpty()) {
                InventoryReleasedEvent releasedEvent = InventoryReleasedEvent.builder()
                        .orderId(orderId)
                        .releasedItems(releasedItems)
                        .occurredAt(Instant.now())
                        .build();

                inventoryEventProducer.publishInventoryReleased(releasedEvent);
                outboxService.saveEvent("INVENTORY", orderId.toString(), KafkaTopics.INVENTORY_RELEASED, releasedEvent);
                log.info("Successfully compensated inventory for order {}. Released {} items.", orderId, releasedItems.size());
            }

            if (eventId != null) {
                idempotencyService.markAsProcessed(eventId, eventType, CONSUMER_GROUP);
            }
        } catch (Exception ex) {
            log.error("Failed to execute inventory compensation for order {}: {}", orderId, ex.getMessage(), ex);
        }
    }
}
