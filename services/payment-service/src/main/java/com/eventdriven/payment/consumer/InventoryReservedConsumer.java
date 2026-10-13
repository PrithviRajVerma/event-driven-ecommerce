package com.eventdriven.payment.consumer;

import com.eventdriven.events.KafkaTopics;
import com.eventdriven.events.inventory.InventoryReservedEvent;
import com.eventdriven.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryReservedConsumer {

    private final PaymentService paymentService;

    @KafkaListener(
            topics = KafkaTopics.INVENTORY_RESERVED,
            groupId = "${spring.kafka.consumer.group-id:payment-service-group}"
    )
    public void handleInventoryReserved(InventoryReservedEvent event) {
        if (event == null || event.getOrderId() == null) {
            log.warn("Received invalid or null InventoryReservedEvent: {}", event);
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
            log.info("Successfully initiated payment processing for orderId: {}", event.getOrderId());
        } catch (Exception ex) {
            log.error("Failed to process payment for orderId {}: {}", event.getOrderId(), ex.getMessage(), ex);
        }
    }
}
