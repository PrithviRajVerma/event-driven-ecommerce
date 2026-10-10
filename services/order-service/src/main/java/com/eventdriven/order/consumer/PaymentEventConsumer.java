package com.eventdriven.order.consumer;

import com.eventdriven.events.KafkaTopics;
import com.eventdriven.events.payment.PaymentCompletedEvent;
import com.eventdriven.events.payment.PaymentFailedEvent;
import com.eventdriven.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = KafkaTopics.PAYMENT_COMPLETED,
            groupId = "${spring.kafka.consumer.group-id:order-service-group}"
    )
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        if (event == null || event.getOrderId() == null) {
            log.warn("Received invalid or null PaymentCompletedEvent: {}", event);
            return;
        }

        log.info("Received PaymentCompletedEvent for orderId: {}, paymentId: {}", event.getOrderId(), event.getPaymentId());
        try {
            orderService.handlePaymentCompleted(event.getOrderId());
        } catch (Exception ex) {
            log.error("Error processing PaymentCompletedEvent for order {}: {}", event.getOrderId(), ex.getMessage(), ex);
        }
    }

    @KafkaListener(
            topics = KafkaTopics.PAYMENT_FAILED,
            groupId = "${spring.kafka.consumer.group-id:order-service-group}"
    )
    public void handlePaymentFailed(PaymentFailedEvent event) {
        if (event == null || event.getOrderId() == null) {
            log.warn("Received invalid or null PaymentFailedEvent: {}", event);
            return;
        }

        log.info("Received PaymentFailedEvent for orderId: {}, reason: {}", event.getOrderId(), event.getReason());
        try {
            orderService.handlePaymentFailed(event.getOrderId(), event.getReason());
        } catch (Exception ex) {
            log.error("Error processing PaymentFailedEvent for order {}: {}", event.getOrderId(), ex.getMessage(), ex);
        }
    }
}
