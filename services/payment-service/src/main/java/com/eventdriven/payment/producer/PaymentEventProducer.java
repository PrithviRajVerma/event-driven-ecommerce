package com.eventdriven.payment.producer;

import com.eventdriven.events.payment.PaymentCompletedEvent;
import com.eventdriven.events.payment.PaymentFailedEvent;
import com.eventdriven.payment.config.KafkaTopicConfig;
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
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CompletableFuture<SendResult<String, Object>> publishPaymentCompleted(PaymentCompletedEvent event) {
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID());
        }
        if (event.getOccurredAt() == null) {
            event.setOccurredAt(Instant.now());
        }
        String key = event.getOrderId().toString();
        log.info("Publishing PaymentCompletedEvent to topic {} with key {}", KafkaTopicConfig.PAYMENT_COMPLETED_TOPIC, key);
        return sendEvent(KafkaTopicConfig.PAYMENT_COMPLETED_TOPIC, key, event);
    }

    public CompletableFuture<SendResult<String, Object>> publishPaymentFailed(PaymentFailedEvent event) {
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID());
        }
        if (event.getOccurredAt() == null) {
            event.setOccurredAt(Instant.now());
        }
        String key = event.getOrderId().toString();
        log.info("Publishing PaymentFailedEvent to topic {} with key {}", KafkaTopicConfig.PAYMENT_FAILED_TOPIC, key);
        return sendEvent(KafkaTopicConfig.PAYMENT_FAILED_TOPIC, key, event);
    }

    private CompletableFuture<SendResult<String, Object>> sendEvent(String topic, String key, Object payload) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, payload);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish message to topic {} with key {}: {}", topic, key, ex.getMessage(), ex);
            } else {
                log.debug("Message published successfully to topic {} partition {} offset {}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
        return future;
    }
}
