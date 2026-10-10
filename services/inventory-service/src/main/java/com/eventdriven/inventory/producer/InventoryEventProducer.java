package com.eventdriven.inventory.producer;

import com.eventdriven.events.inventory.InventoryReleasedEvent;
import com.eventdriven.events.inventory.InventoryReservedEvent;
import com.eventdriven.events.inventory.ReservationFailedEvent;
import com.eventdriven.inventory.config.KafkaTopicConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CompletableFuture<SendResult<String, Object>> publishInventoryReserved(InventoryReservedEvent event) {
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID());
        }
        if (event.getOccurredAt() == null) {
            event.setOccurredAt(Instant.now());
        }
        String key = event.getOrderId().toString();
        log.info("Publishing InventoryReservedEvent to topic {} with key {}", KafkaTopicConfig.INVENTORY_RESERVED_TOPIC, key);
        return sendEvent(KafkaTopicConfig.INVENTORY_RESERVED_TOPIC, key, event);
    }

    public CompletableFuture<SendResult<String, Object>> publishReservationFailed(ReservationFailedEvent event) {
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID());
        }
        if (event.getOccurredAt() == null) {
            event.setOccurredAt(Instant.now());
        }
        String key = event.getOrderId().toString();
        log.info("Publishing ReservationFailedEvent to topic {} with key {}", KafkaTopicConfig.RESERVATION_FAILED_TOPIC, key);
        return sendEvent(KafkaTopicConfig.RESERVATION_FAILED_TOPIC, key, event);
    }

    public CompletableFuture<SendResult<String, Object>> publishInventoryReleased(InventoryReleasedEvent event) {
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID());
        }
        if (event.getOccurredAt() == null) {
            event.setOccurredAt(Instant.now());
        }
        String key = event.getOrderId().toString();
        log.info("Publishing InventoryReleasedEvent to topic {} with key {}", KafkaTopicConfig.INVENTORY_RELEASED_TOPIC, key);
        return sendEvent(KafkaTopicConfig.INVENTORY_RELEASED_TOPIC, key, event);
    }

    private CompletableFuture<SendResult<String, Object>> sendEvent(String topic, String key, Object payload) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, payload);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish message to topic {} with key {}: {}", topic, key, ex.getMessage(), ex);
            } else {
                log.info("Successfully published message to topic {} [partition: {}, offset: {}] with key {}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        key);
            }
        });
        return future;
    }
}
