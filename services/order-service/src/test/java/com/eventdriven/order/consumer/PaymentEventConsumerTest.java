package com.eventdriven.order.consumer;

import com.eventdriven.events.payment.PaymentCompletedEvent;
import com.eventdriven.events.payment.PaymentFailedEvent;
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

    @InjectMocks
    private PaymentEventConsumer paymentEventConsumer;

    @Test
    @DisplayName("handlePaymentCompleted calls orderService.handlePaymentCompleted")
    void handlePaymentCompleted_Success() {
        UUID orderId = UUID.randomUUID();
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .orderId(orderId)
                .paymentId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .amount(new BigDecimal("99.99"))
                .currency("USD")
                .build();

        paymentEventConsumer.handlePaymentCompleted(event);

        verify(orderService).handlePaymentCompleted(orderId);
    }

    @Test
    @DisplayName("handlePaymentFailed calls orderService.handlePaymentFailed")
    void handlePaymentFailed_Success() {
        UUID orderId = UUID.randomUUID();
        String reason = "Card declined: insufficient funds";
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .orderId(orderId)
                .amount(new BigDecimal("99.99"))
                .reason(reason)
                .build();

        paymentEventConsumer.handlePaymentFailed(event);

        verify(orderService).handlePaymentFailed(orderId, reason);
    }

    @Test
    @DisplayName("Null or empty events are ignored safely")
    void handleEvents_NullIgnored() {
        paymentEventConsumer.handlePaymentCompleted(null);
        paymentEventConsumer.handlePaymentCompleted(new PaymentCompletedEvent());

        paymentEventConsumer.handlePaymentFailed(null);
        paymentEventConsumer.handlePaymentFailed(new PaymentFailedEvent());

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("Exceptions in orderService are caught without terminating listener")
    void handlePaymentCompleted_CatchesException() {
        UUID orderId = UUID.randomUUID();
        PaymentCompletedEvent event = PaymentCompletedEvent.builder().orderId(orderId).build();

        doThrow(new RuntimeException("DB down")).when(orderService).handlePaymentCompleted(orderId);

        paymentEventConsumer.handlePaymentCompleted(event);

        verify(orderService).handlePaymentCompleted(orderId);
    }
}
