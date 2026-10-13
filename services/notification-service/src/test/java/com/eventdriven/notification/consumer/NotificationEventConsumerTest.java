package com.eventdriven.notification.consumer;

import com.eventdriven.events.order.OrderCancelledEvent;
import com.eventdriven.events.order.OrderCreatedEvent;
import com.eventdriven.events.payment.PaymentCompletedEvent;
import com.eventdriven.events.payment.PaymentFailedEvent;
import com.eventdriven.notification.idempotency.IdempotencyService;
import com.eventdriven.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventConsumerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private NotificationEventConsumer consumer;

    @Test
    @DisplayName("handleOrderCreated processes and records idempotency")
    void handleOrderCreated_Success() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .customerId(UUID.randomUUID())
                .totalAmount(new BigDecimal("99.00"))
                .build();

        when(idempotencyService.isAlreadyProcessed(eventId)).thenReturn(false);

        consumer.handleOrderCreated(event);

        verify(notificationService).handleOrderCreated(event);
        verify(idempotencyService).markAsProcessed(eq(eventId), any(), eq("notification-service-group"));
    }

    @Test
    @DisplayName("handleOrderCreated skips duplicate event safely")
    void handleOrderCreated_Duplicate_Skipped() {
        UUID eventId = UUID.randomUUID();
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(eventId)
                .orderId(UUID.randomUUID())
                .build();

        when(idempotencyService.isAlreadyProcessed(eventId)).thenReturn(true);

        consumer.handleOrderCreated(event);

        verifyNoInteractions(notificationService);
        verify(idempotencyService, never()).markAsProcessed(any(), any(), any());
    }

    @Test
    @DisplayName("handlePaymentCompleted processes and records idempotency")
    void handlePaymentCompleted_Success() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .customerId(UUID.randomUUID())
                .paymentId(UUID.randomUUID())
                .amount(new BigDecimal("150.00"))
                .currency("USD")
                .occurredAt(Instant.now())
                .build();

        when(idempotencyService.isAlreadyProcessed(eventId)).thenReturn(false);

        consumer.handlePaymentCompleted(event);

        verify(notificationService).handlePaymentCompleted(event);
        verify(idempotencyService).markAsProcessed(eq(eventId), any(), eq("notification-service-group"));
    }

    @Test
    @DisplayName("handlePaymentFailed processes and records idempotency")
    void handlePaymentFailed_Success() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .customerId(UUID.randomUUID())
                .amount(new BigDecimal("200.00"))
                .reason("Card expired")
                .occurredAt(Instant.now())
                .build();

        when(idempotencyService.isAlreadyProcessed(eventId)).thenReturn(false);

        consumer.handlePaymentFailed(event);

        verify(notificationService).handlePaymentFailed(event);
        verify(idempotencyService).markAsProcessed(eq(eventId), any(), eq("notification-service-group"));
    }

    @Test
    @DisplayName("handleOrderCancelled processes and records idempotency")
    void handleOrderCancelled_Success() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .customerId(UUID.randomUUID())
                .reason("Customer request")
                .build();

        when(idempotencyService.isAlreadyProcessed(eventId)).thenReturn(false);

        consumer.handleOrderCancelled(event);

        verify(notificationService).handleOrderCancelled(event);
        verify(idempotencyService).markAsProcessed(eq(eventId), any(), eq("notification-service-group"));
    }

    @Test
    @DisplayName("Null event handled safely without throwing")
    void handleOrderCreated_NullEvent_Safe() {
        consumer.handleOrderCreated(null);
        verifyNoInteractions(notificationService);
        verifyNoInteractions(idempotencyService);
    }
}
