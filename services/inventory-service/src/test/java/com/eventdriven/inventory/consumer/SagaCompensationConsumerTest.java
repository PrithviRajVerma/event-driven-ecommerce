package com.eventdriven.inventory.consumer;

import com.eventdriven.events.inventory.InventoryReleasedEvent;
import com.eventdriven.events.order.OrderCancelledEvent;
import com.eventdriven.events.payment.PaymentFailedEvent;
import com.eventdriven.inventory.idempotency.IdempotencyService;
import com.eventdriven.inventory.outbox.OutboxService;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SagaCompensationConsumerTest {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private InventoryEventProducer inventoryEventProducer;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private SagaCompensationConsumer sagaCompensationConsumer;

    @Test
    @DisplayName("handlePaymentFailed releases inventory, publishes InventoryReleasedEvent, and records idempotency")
    void handlePaymentFailed_Success() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .amount(new BigDecimal("50.00"))
                .reason("Card declined")
                .build();

        when(idempotencyService.isAlreadyProcessed(eventId)).thenReturn(false);

        List<InventoryReleasedEvent.ReleasedItem> released = List.of(
                InventoryReleasedEvent.ReleasedItem.builder().productId(productId).quantity(2).build()
        );
        when(inventoryService.releaseOrderInventory(orderId, "Card declined")).thenReturn(released);

        sagaCompensationConsumer.handlePaymentFailed(event);

        verify(inventoryService).releaseOrderInventory(orderId, "Card declined");

        ArgumentCaptor<InventoryReleasedEvent> captor = ArgumentCaptor.forClass(InventoryReleasedEvent.class);
        verify(inventoryEventProducer).publishInventoryReleased(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo(orderId);
        assertThat(captor.getValue().getReleasedItems()).hasSize(1);

        verify(outboxService).saveEvent(eq("INVENTORY"), eq(orderId.toString()), any(), any());
        verify(idempotencyService).markAsProcessed(eq(eventId), any(), any());
    }

    @Test
    @DisplayName("handlePaymentFailed skips duplicate event")
    void handlePaymentFailed_Duplicate_Skipped() {
        UUID eventId = UUID.randomUUID();
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .eventId(eventId)
                .orderId(UUID.randomUUID())
                .build();

        when(idempotencyService.isAlreadyProcessed(eventId)).thenReturn(true);

        sagaCompensationConsumer.handlePaymentFailed(event);

        verifyNoInteractions(inventoryService, inventoryEventProducer);
    }

    @Test
    @DisplayName("handleOrderCancelled triggers inventory release")
    void handleOrderCancelled_Success() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .reason("Customer cancelled")
                .build();

        when(idempotencyService.isAlreadyProcessed(eventId)).thenReturn(false);
        when(inventoryService.releaseOrderInventory(orderId, "Customer cancelled")).thenReturn(List.of());

        sagaCompensationConsumer.handleOrderCancelled(event);

        verify(inventoryService).releaseOrderInventory(orderId, "Customer cancelled");
        verify(idempotencyService).markAsProcessed(eq(eventId), any(), any());
    }

    @Test
    @DisplayName("Safely ignores null event")
    void handleNull_SafelyIgnored() {
        sagaCompensationConsumer.handlePaymentFailed(null);
        sagaCompensationConsumer.handleOrderCancelled(null);

        verifyNoInteractions(inventoryService, inventoryEventProducer);
    }
}
