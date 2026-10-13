package com.eventdriven.payment.consumer;

import com.eventdriven.events.KafkaTopics;
import com.eventdriven.events.inventory.InventoryReservedEvent;
import com.eventdriven.payment.idempotency.IdempotencyService;
import com.eventdriven.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryReservedConsumer {

    private static final String CONSUMER_GROUP = "payment-service-group";

    private final PaymentService paymentService;
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
            log.warn("Duplicate InventoryReservedEvent {} detected for order {}. Skipping.", event.getEventId(), event.getOrderId());
            return;
        }

        log.info("Received InventoryReservedEvent for orderId: {}, customerId: {}, totalAmount: {}",
                event.getOrderId(), event.getCustomerId(), event.getTotalAmount());

        try {
            paymentService.processOrderPayment(
                    event.getOrderId(),
                    event.getCustomerId(),
                    event.getTotalAmount(),
                    "USD"
            );
            if (event.getEventId() != null) {
                idempotencyService.markAsProcessed(event.getEventId(), event.getEventType(), CONSUMER_GROUP);
            }
            log.info("Successfully initiated payment processing for orderId: {}", event.getOrderId());
        } catch (Exception ex) {
            log.error("Failed to process payment for orderId {}: {}", event.getOrderId(), ex.getMessage(), ex);
        }
    }
}
