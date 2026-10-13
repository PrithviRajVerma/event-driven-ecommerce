package com.eventdriven.order.consumer;

import com.eventdriven.events.inventory.InventoryReservedEvent;
import com.eventdriven.events.inventory.ReservationFailedEvent;
import com.eventdriven.order.idempotency.IdempotencyService;
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

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private InventoryEventConsumer inventoryEventConsumer;

    @Test
    @DisplayName("handleInventoryReserved calls orderService.handleInventoryReserved and records idempotency")
    void handleInventoryReserved_Success() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        InventoryReservedEvent event = InventoryReservedEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .customerId(UUID.randomUUID())
                .totalAmount(new BigDecimal("100.00"))
                .reservedItems(List.of())
                .build();

        when(idempotencyService.isAlreadyProcessed(eventId)).thenReturn(false);

        inventoryEventConsumer.handleInventoryReserved(event);

        verify(orderService).handleInventoryReserved(orderId);
        verify(idempotencyService).markAsProcessed(eventId, event.getEventType(), "order-service-group");
    }

    @Test
    @DisplayName("handleInventoryReserved ignores duplicate event when already processed")
    void handleInventoryReserved_Duplicate_Skipped() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        InventoryReservedEvent event = InventoryReservedEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .build();

        when(idempotencyService.isAlreadyProcessed(eventId)).thenReturn(true);

        inventoryEventConsumer.handleInventoryReserved(event);

        verifyNoInteractions(orderService);
        verify(idempotencyService, never()).markAsProcessed(any(), any(), any());
    }

    @Test
    @DisplayName("handleReservationFailed calls orderService.handleInventoryReservationFailed and records idempotency")
    void handleReservationFailed_Success() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        ReservationFailedEvent event = ReservationFailedEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .reason("Insufficient stock")
                .build();

        when(idempotencyService.isAlreadyProcessed(eventId)).thenReturn(false);

        inventoryEventConsumer.handleReservationFailed(event);

        verify(orderService).handleInventoryReservationFailed(orderId, "Insufficient stock");
        verify(idempotencyService).markAsProcessed(eventId, event.getEventType(), "order-service-group");
    }

    @Test
    @DisplayName("handleReservationFailed ignores duplicate event when already processed")
    void handleReservationFailed_Duplicate_Skipped() {
        UUID eventId = UUID.randomUUID();
        ReservationFailedEvent event = ReservationFailedEvent.builder()
                .eventId(eventId)
                .orderId(UUID.randomUUID())
                .reason("Out of stock")
                .build();

        when(idempotencyService.isAlreadyProcessed(eventId)).thenReturn(true);

        inventoryEventConsumer.handleReservationFailed(event);

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("handleInventoryReserved ignores null or empty orderId safely")
    void handleInventoryReserved_Null_SafelyIgnored() {
        inventoryEventConsumer.handleInventoryReserved(null);
        inventoryEventConsumer.handleInventoryReserved(InventoryReservedEvent.builder().build());

        verifyNoInteractions(orderService);
    }
}
