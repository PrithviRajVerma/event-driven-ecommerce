package com.eventdriven.order.consumer;

import com.eventdriven.events.payment.PaymentCompletedEvent;
import com.eventdriven.events.payment.PaymentFailedEvent;
import com.eventdriven.order.idempotency.IdempotencyService;
import com.eventdriven.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventConsumerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private PaymentEventConsumer paymentEventConsumer;

    @Test
    @DisplayName("handlePaymentCompleted calls orderService.handlePaymentCompleted and records idempotency")
    void handlePaymentCompleted_Success() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .paymentId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .amount(new BigDecimal("99.99"))
                .currency("USD")
                .build();

        when(idempotencyService.isAlreadyProcessed(eventId)).thenReturn(false);

        paymentEventConsumer.handlePaymentCompleted(event);

        verify(orderService).handlePaymentCompleted(orderId);
        verify(idempotencyService).markAsProcessed(eventId, event.getEventType(), "order-service-group");
    }

    @Test
    @DisplayName("handlePaymentCompleted ignores duplicate event when already processed")
    void handlePaymentCompleted_Duplicate_Skipped() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .build();

        when(idempotencyService.isAlreadyProcessed(eventId)).thenReturn(true);

        paymentEventConsumer.handlePaymentCompleted(event);

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("handlePaymentFailed calls orderService.handlePaymentFailed and records idempotency")
    void handlePaymentFailed_Success() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .customerId(UUID.randomUUID())
                .amount(new BigDecimal("50.00"))
                .reason("Insufficient funds")
                .build();

        when(idempotencyService.isAlreadyProcessed(eventId)).thenReturn(false);

        paymentEventConsumer.handlePaymentFailed(event);

        verify(orderService).handlePaymentFailed(orderId, "Insufficient funds");
        verify(idempotencyService).markAsProcessed(eventId, event.getEventType(), "order-service-group");
    }

    @Test
    @DisplayName("handlePaymentFailed ignores duplicate event when already processed")
    void handlePaymentFailed_Duplicate_Skipped() {
        UUID eventId = UUID.randomUUID();
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .eventId(eventId)
                .orderId(UUID.randomUUID())
                .reason("Card expired")
                .build();

        when(idempotencyService.isAlreadyProcessed(eventId)).thenReturn(true);

        paymentEventConsumer.handlePaymentFailed(event);

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("handlePaymentCompleted ignores null or empty orderId safely")
    void handlePaymentCompleted_Null_SafelyIgnored() {
        paymentEventConsumer.handlePaymentCompleted(null);
        paymentEventConsumer.handlePaymentCompleted(PaymentCompletedEvent.builder().build());

        verifyNoInteractions(orderService);
    }
}
