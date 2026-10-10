package com.eventdriven.inventory.consumer;

import com.eventdriven.events.inventory.InventoryReservedEvent;
import com.eventdriven.events.inventory.ReservationFailedEvent;
import com.eventdriven.events.order.OrderCreatedEvent;
import com.eventdriven.inventory.exception.InsufficientStockException;
import com.eventdriven.inventory.exception.InventoryNotFoundException;
import com.eventdriven.inventory.producer.InventoryEventProducer;
import com.eventdriven.inventory.service.InventoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCreatedConsumerTest {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private InventoryEventProducer inventoryEventProducer;

    @InjectMocks
    private OrderCreatedConsumer orderCreatedConsumer;

    @Test
    @DisplayName("Successfully reserves inventory and publishes InventoryReservedEvent")
    void handleOrderCreated_Success() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(orderId)
                .customerId(customerId)
                .totalAmount(new BigDecimal("120.00"))
                .items(List.of(
                        OrderCreatedEvent.OrderItem.builder().productId(productId1).quantity(2).unitPrice(new BigDecimal("50.00")).build(),
                        OrderCreatedEvent.OrderItem.builder().productId(productId2).quantity(1).unitPrice(new BigDecimal("20.00")).build()
                ))
                .build();

        List<InventoryReservedEvent.ReservedItem> reservedItems = List.of(
                InventoryReservedEvent.ReservedItem.builder().productId(productId1).quantity(2).build(),
                InventoryReservedEvent.ReservedItem.builder().productId(productId2).quantity(1).build()
        );

        when(inventoryService.reserveOrderInventory(eq(orderId), eq(event.getItems()))).thenReturn(reservedItems);

        orderCreatedConsumer.handleOrderCreated(event);

        verify(inventoryService).reserveOrderInventory(orderId, event.getItems());

        ArgumentCaptor<InventoryReservedEvent> captor = ArgumentCaptor.forClass(InventoryReservedEvent.class);
        verify(inventoryEventProducer).publishInventoryReserved(captor.capture());
        verify(inventoryEventProducer, never()).publishReservationFailed(any());

        InventoryReservedEvent captured = captor.getValue();
        assertThat(captured.getOrderId()).isEqualTo(orderId);
        assertThat(captured.getCustomerId()).isEqualTo(customerId);
        assertThat(captured.getTotalAmount()).isEqualTo(new BigDecimal("120.00"));
        assertThat(captured.getReservedItems()).hasSize(2);
    }

    @Test
    @DisplayName("Publishes ReservationFailedEvent when stock is insufficient")
    void handleOrderCreated_InsufficientStock() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(orderId)
                .customerId(customerId)
                .totalAmount(new BigDecimal("50.00"))
                .items(List.of(
                        OrderCreatedEvent.OrderItem.builder().productId(productId).quantity(5).unitPrice(new BigDecimal("10.00")).build()
                ))
                .build();

        when(inventoryService.reserveOrderInventory(eq(orderId), eq(event.getItems())))
                .thenThrow(new InsufficientStockException("Insufficient stock for product " + productId));

        orderCreatedConsumer.handleOrderCreated(event);

        verify(inventoryService).reserveOrderInventory(orderId, event.getItems());
        verify(inventoryEventProducer, never()).publishInventoryReserved(any());

        ArgumentCaptor<ReservationFailedEvent> captor = ArgumentCaptor.forClass(ReservationFailedEvent.class);
        verify(inventoryEventProducer).publishReservationFailed(captor.capture());

        ReservationFailedEvent captured = captor.getValue();
        assertThat(captured.getOrderId()).isEqualTo(orderId);
        assertThat(captured.getReason()).contains("Insufficient stock");
    }

    @Test
    @DisplayName("Publishes ReservationFailedEvent when product inventory is not found")
    void handleOrderCreated_InventoryNotFound() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(orderId)
                .customerId(customerId)
                .totalAmount(new BigDecimal("50.00"))
                .items(List.of(
                        OrderCreatedEvent.OrderItem.builder().productId(productId).quantity(1).unitPrice(new BigDecimal("50.00")).build()
                ))
                .build();

        when(inventoryService.reserveOrderInventory(eq(orderId), eq(event.getItems())))
                .thenThrow(new InventoryNotFoundException("Inventory not found for product: " + productId));

        orderCreatedConsumer.handleOrderCreated(event);

        verify(inventoryService).reserveOrderInventory(orderId, event.getItems());
        verify(inventoryEventProducer, never()).publishInventoryReserved(any());

        ArgumentCaptor<ReservationFailedEvent> captor = ArgumentCaptor.forClass(ReservationFailedEvent.class);
        verify(inventoryEventProducer).publishReservationFailed(captor.capture());

        ReservationFailedEvent captured = captor.getValue();
        assertThat(captured.getOrderId()).isEqualTo(orderId);
        assertThat(captured.getReason()).contains("Inventory not found");
    }

    @Test
    @DisplayName("Ignores null or invalid event without calling service or producer")
    void handleOrderCreated_NullEvent() {
        orderCreatedConsumer.handleOrderCreated(null);
        verifyNoInteractions(inventoryService, inventoryEventProducer);

        OrderCreatedEvent invalidEvent = new OrderCreatedEvent();
        orderCreatedConsumer.handleOrderCreated(invalidEvent);
        verifyNoInteractions(inventoryService, inventoryEventProducer);
    }
}
