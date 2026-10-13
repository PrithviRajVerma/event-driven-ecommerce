package com.eventdriven.order.consumer;

import com.eventdriven.events.KafkaTopics;
import com.eventdriven.events.payment.PaymentCompletedEvent;
import com.eventdriven.events.payment.PaymentFailedEvent;
import com.eventdriven.order.idempotency.IdempotencyService;
import com.eventdriven.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private static final String CONSUMER_GROUP = "order-service-group";

    private final OrderService orderService;
    private final IdempotencyService idempotencyService;

    @KafkaListener(
            topics = KafkaTopics.PAYMENT_COMPLETED,
            groupId = "${spring.kafka.consumer.group-id:" + CONSUMER_GROUP + "}"
    )
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        if (event == null || event.getOrderId() == null) {
            log.warn("Received invalid or null PaymentCompletedEvent: {}", event);
            return;
        }

        if (event.getEventId() != null && idempotencyService.isAlreadyProcessed(event.getEventId())) {
            log.warn("Duplicate event {} detected in PaymentCompletedEvent. Skipping.", event.getEventId());
            return;
        }

        log.info("Received PaymentCompletedEvent for orderId: {}, paymentId: {}", event.getOrderId(), event.getPaymentId());
        try {
            orderService.handlePaymentCompleted(event.getOrderId());
            if (event.getEventId() != null) {
                idempotencyService.markAsProcessed(event.getEventId(), event.getEventType(), CONSUMER_GROUP);
            }
        } catch (Exception ex) {
            log.error("Error processing PaymentCompletedEvent for order {}: {}", event.getOrderId(), ex.getMessage(), ex);
        }
    }

    @KafkaListener(
            topics = KafkaTopics.PAYMENT_FAILED,
            groupId = "${spring.kafka.consumer.group-id:" + CONSUMER_GROUP + "}"
    )
    public void handlePaymentFailed(PaymentFailedEvent event) {
        if (event == null || event.getOrderId() == null) {
            log.warn("Received invalid or null PaymentFailedEvent: {}", event);
            return;
        }

        if (event.getEventId() != null && idempotencyService.isAlreadyProcessed(event.getEventId())) {
            log.warn("Duplicate event {} detected in PaymentFailedEvent. Skipping.", event.getEventId());
            return;
        }

        log.info("Received PaymentFailedEvent for orderId: {}, reason: {}", event.getOrderId(), event.getReason());
        try {
            orderService.handlePaymentFailed(event.getOrderId(), event.getReason());
            if (event.getEventId() != null) {
                idempotencyService.markAsProcessed(event.getEventId(), event.getEventType(), CONSUMER_GROUP);
            }
        } catch (Exception ex) {
            log.error("Error processing PaymentFailedEvent for order {}: {}", event.getOrderId(), ex.getMessage(), ex);
        }
    }
}
