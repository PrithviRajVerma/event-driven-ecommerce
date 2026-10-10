package com.eventdriven.order.producer;

import com.eventdriven.events.order.OrderCancelledEvent;
import com.eventdriven.events.order.OrderConfirmedEvent;
import com.eventdriven.events.order.OrderCreatedEvent;
import com.eventdriven.order.config.KafkaTopicConfig;
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
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CompletableFuture<SendResult<String, Object>> publishOrderCreated(OrderCreatedEvent event) {
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID());
        }
        if (event.getOccurredAt() == null) {
            event.setOccurredAt(Instant.now());
        }
        String key = event.getOrderId().toString();
        log.info("Publishing OrderCreatedEvent to topic {} with key {}", KafkaTopicConfig.ORDER_CREATED_TOPIC, key);
        return sendEvent(KafkaTopicConfig.ORDER_CREATED_TOPIC, key, event);
    }

    public CompletableFuture<SendResult<String, Object>> publishOrderConfirmed(OrderConfirmedEvent event) {
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID());
        }
        if (event.getOccurredAt() == null) {
            event.setOccurredAt(Instant.now());
        }
        String key = event.getOrderId().toString();
        log.info("Publishing OrderConfirmedEvent to topic {} with key {}", KafkaTopicConfig.ORDER_CONFIRMED_TOPIC, key);
        return sendEvent(KafkaTopicConfig.ORDER_CONFIRMED_TOPIC, key, event);
    }

    public CompletableFuture<SendResult<String, Object>> publishOrderCancelled(OrderCancelledEvent event) {
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID());
        }
        if (event.getOccurredAt() == null) {
            event.setOccurredAt(Instant.now());
        }
        String key = event.getOrderId().toString();
        log.info("Publishing OrderCancelledEvent to topic {} with key {}", KafkaTopicConfig.ORDER_CANCELLED_TOPIC, key);
        return sendEvent(KafkaTopicConfig.ORDER_CANCELLED_TOPIC, key, event);
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
