package com.eventdriven.payment.consumer;

import com.eventdriven.events.inventory.InventoryReservedEvent;
import com.eventdriven.payment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryReservedConsumerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private InventoryReservedConsumer inventoryReservedConsumer;

    @Test
    @DisplayName("Successfully processes payment when InventoryReservedEvent received")
    void handleInventoryReserved_Success() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        BigDecimal totalAmount = new BigDecimal("89.99");

        InventoryReservedEvent event = InventoryReservedEvent.builder()
                .orderId(orderId)
                .customerId(customerId)
                .totalAmount(totalAmount)
                .reservedItems(List.of())
                .build();

        inventoryReservedConsumer.handleInventoryReserved(event);

        verify(paymentService).processOrderPayment(orderId, customerId, totalAmount, "USD");
    }

    @Test
    @DisplayName("Ignores null event safely")
    void handleInventoryReserved_NullEvent_Ignores() {
        inventoryReservedConsumer.handleInventoryReserved(null);
        verifyNoInteractions(paymentService);
    }

    @Test
    @DisplayName("Ignores event with null orderId safely")
    void handleInventoryReserved_NullOrderId_Ignores() {
        InventoryReservedEvent event = InventoryReservedEvent.builder()
                .orderId(null)
                .customerId(UUID.randomUUID())
                .build();

        inventoryReservedConsumer.handleInventoryReserved(event);
        verifyNoInteractions(paymentService);
    }

    @Test
    @DisplayName("Catches and logs exceptions thrown during payment processing without crashing")
    void handleInventoryReserved_Exception_HandledGracefully() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        BigDecimal totalAmount = new BigDecimal("50.00");

        InventoryReservedEvent event = InventoryReservedEvent.builder()
                .orderId(orderId)
                .customerId(customerId)
                .totalAmount(totalAmount)
                .build();

        doThrow(new RuntimeException("Simulated processing error"))
                .when(paymentService).processOrderPayment(any(), any(), any(), any());

        // Should not throw or crash
        inventoryReservedConsumer.handleInventoryReserved(event);

        verify(paymentService).processOrderPayment(orderId, customerId, totalAmount, "USD");
    }
}
