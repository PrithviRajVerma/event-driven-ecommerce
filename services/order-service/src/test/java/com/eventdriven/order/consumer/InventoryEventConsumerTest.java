package com.eventdriven.order.consumer;

import com.eventdriven.events.inventory.InventoryReservedEvent;
import com.eventdriven.events.inventory.ReservationFailedEvent;
import com.eventdriven.order.service.OrderService;
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
class InventoryEventConsumerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private InventoryEventConsumer inventoryEventConsumer;

    @Test
    @DisplayName("handleInventoryReserved calls orderService.handleInventoryReserved")
    void handleInventoryReserved_Success() {
        UUID orderId = UUID.randomUUID();
        InventoryReservedEvent event = InventoryReservedEvent.builder()
                .orderId(orderId)
                .customerId(UUID.randomUUID())
                .totalAmount(new BigDecimal("100.00"))
                .reservedItems(List.of())
                .build();

        inventoryEventConsumer.handleInventoryReserved(event);

        verify(orderService).handleInventoryReserved(orderId);
    }

    @Test
    @DisplayName("handleReservationFailed calls orderService.handleInventoryReservationFailed")
    void handleReservationFailed_Success() {
        UUID orderId = UUID.randomUUID();
        String reason = "Out of stock for SKU 123";
        ReservationFailedEvent event = ReservationFailedEvent.builder()
                .orderId(orderId)
                .reason(reason)
                .build();

        inventoryEventConsumer.handleReservationFailed(event);

        verify(orderService).handleInventoryReservationFailed(orderId, reason);
    }

    @Test
    @DisplayName("Null or empty events are ignored safely")
    void handleEvents_NullIgnored() {
        inventoryEventConsumer.handleInventoryReserved(null);
        inventoryEventConsumer.handleInventoryReserved(new InventoryReservedEvent());

        inventoryEventConsumer.handleReservationFailed(null);
        inventoryEventConsumer.handleReservationFailed(new ReservationFailedEvent());

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("Exceptions in orderService are caught without terminating listener")
    void handleInventoryReserved_CatchesException() {
        UUID orderId = UUID.randomUUID();
        InventoryReservedEvent event = InventoryReservedEvent.builder().orderId(orderId).build();

        doThrow(new RuntimeException("DB down")).when(orderService).handleInventoryReserved(orderId);

        inventoryEventConsumer.handleInventoryReserved(event);

        verify(orderService).handleInventoryReserved(orderId);
    }
}
