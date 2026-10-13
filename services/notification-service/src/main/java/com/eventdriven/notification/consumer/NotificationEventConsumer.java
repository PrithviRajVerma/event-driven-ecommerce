package com.eventdriven.notification.consumer;

import com.eventdriven.events.KafkaTopics;
import com.eventdriven.events.order.OrderCancelledEvent;
import com.eventdriven.events.order.OrderCreatedEvent;
import com.eventdriven.events.payment.PaymentCompletedEvent;
import com.eventdriven.events.payment.PaymentFailedEvent;
import com.eventdriven.notification.idempotency.IdempotencyService;
import com.eventdriven.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private static final String CONSUMER_GROUP = "notification-service-group";

    private final NotificationService notificationService;
    private final IdempotencyService idempotencyService;

    @KafkaListener(
            topics = KafkaTopics.ORDER_CREATED,
            groupId = "${spring.kafka.consumer.group-id:" + CONSUMER_GROUP + "}"
    )
    public void handleOrderCreated(OrderCreatedEvent event) {
        if (event == null || event.getOrderId() == null) {
            log.warn("Received invalid OrderCreatedEvent: {}", event);
            return;
        }

        if (event.getEventId() != null && idempotencyService.isAlreadyProcessed(event.getEventId())) {
            log.warn("Duplicate OrderCreatedEvent {} for order {}. Skipping.", event.getEventId(), event.getOrderId());
            return;
        }

        log.info("Processing notification for OrderCreatedEvent: {}", event.getOrderId());
        try {
            notificationService.handleOrderCreated(event);
            if (event.getEventId() != null) {
                idempotencyService.markAsProcessed(event.getEventId(), event.getEventType(), CONSUMER_GROUP);
            }
        } catch (Exception ex) {
            log.error("Failed to process notification for order {}: {}", event.getOrderId(), ex.getMessage(), ex);
        }
    }

    @KafkaListener(
            topics = KafkaTopics.PAYMENT_COMPLETED,
            groupId = "${spring.kafka.consumer.group-id:" + CONSUMER_GROUP + "}"
    )
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        if (event == null || event.getOrderId() == null) {
            log.warn("Received invalid PaymentCompletedEvent: {}", event);
            return;
        }

        if (event.getEventId() != null && idempotencyService.isAlreadyProcessed(event.getEventId())) {
            log.warn("Duplicate PaymentCompletedEvent {} for order {}. Skipping.", event.getEventId(), event.getOrderId());
            return;
        }

        log.info("Processing notification for PaymentCompletedEvent: {}", event.getOrderId());
        try {
            notificationService.handlePaymentCompleted(event);
            if (event.getEventId() != null) {
                idempotencyService.markAsProcessed(event.getEventId(), event.getEventType(), CONSUMER_GROUP);
            }
        } catch (Exception ex) {
            log.error("Failed to process payment completed notification for order {}: {}", event.getOrderId(), ex.getMessage(), ex);
        }
    }

    @KafkaListener(
            topics = KafkaTopics.PAYMENT_FAILED,
            groupId = "${spring.kafka.consumer.group-id:" + CONSUMER_GROUP + "}"
    )
    public void handlePaymentFailed(PaymentFailedEvent event) {
        if (event == null || event.getOrderId() == null) {
            log.warn("Received invalid PaymentFailedEvent: {}", event);
            return;
        }

        if (event.getEventId() != null && idempotencyService.isAlreadyProcessed(event.getEventId())) {
            log.warn("Duplicate PaymentFailedEvent {} for order {}. Skipping.", event.getEventId(), event.getOrderId());
            return;
        }

        log.info("Processing notification for PaymentFailedEvent: {}", event.getOrderId());
        try {
            notificationService.handlePaymentFailed(event);
            if (event.getEventId() != null) {
                idempotencyService.markAsProcessed(event.getEventId(), event.getEventType(), CONSUMER_GROUP);
            }
        } catch (Exception ex) {
            log.error("Failed to process payment failed notification for order {}: {}", event.getOrderId(), ex.getMessage(), ex);
        }
    }

    @KafkaListener(
            topics = KafkaTopics.ORDER_CANCELLED,
            groupId = "${spring.kafka.consumer.group-id:" + CONSUMER_GROUP + "}"
    )
    public void handleOrderCancelled(OrderCancelledEvent event) {
        if (event == null || event.getOrderId() == null) {
            log.warn("Received invalid OrderCancelledEvent: {}", event);
            return;
        }

        if (event.getEventId() != null && idempotencyService.isAlreadyProcessed(event.getEventId())) {
            log.warn("Duplicate OrderCancelledEvent {} for order {}. Skipping.", event.getEventId(), event.getOrderId());
            return;
        }

        log.info("Processing notification for OrderCancelledEvent: {}", event.getOrderId());
        try {
            notificationService.handleOrderCancelled(event);
            if (event.getEventId() != null) {
                idempotencyService.markAsProcessed(event.getEventId(), event.getEventType(), CONSUMER_GROUP);
            }
        } catch (Exception ex) {
            log.error("Failed to process order cancelled notification for order {}: {}", event.getOrderId(), ex.getMessage(), ex);
        }
    }
}
