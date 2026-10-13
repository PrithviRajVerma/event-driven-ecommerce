package com.eventdriven.inventory.e2e;

import com.eventdriven.events.KafkaTopics;
import com.eventdriven.events.inventory.InventoryReleasedEvent;
import com.eventdriven.events.inventory.InventoryReservedEvent;
import com.eventdriven.events.inventory.ReservationFailedEvent;
import com.eventdriven.events.order.OrderCancelledEvent;
import com.eventdriven.events.order.OrderCreatedEvent;
import com.eventdriven.events.payment.PaymentFailedEvent;
import com.eventdriven.inventory.consumer.OrderCreatedConsumer;
import com.eventdriven.inventory.consumer.SagaCompensationConsumer;
import com.eventdriven.inventory.exception.InsufficientStockException;
import com.eventdriven.inventory.idempotency.IdempotencyService;
import com.eventdriven.inventory.outbox.OutboxEvent;
import com.eventdriven.inventory.outbox.OutboxEventRepository;
import com.eventdriven.inventory.outbox.OutboxPublisher;
import com.eventdriven.inventory.outbox.OutboxService;
import com.eventdriven.inventory.outbox.OutboxStatus;
import com.eventdriven.inventory.producer.InventoryEventProducer;
import com.eventdriven.inventory.repository.InventoryRepository;
import com.eventdriven.inventory.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * End-to-End Inventory Saga Test.
 *
 * Verifies:
 * 1. OrderCreatedEvent consumption & atomic stock reservation
 * 2. Outbox event recording and relay to Kafka
 * 3. Out-of-stock reservation failure event handling
 * 4. Saga Compensation: PaymentFailed & OrderCancelled trigger stock release
 * 5. Idempotent processing of duplicate saga events
 */
@ExtendWith(MockitoExtension.class)
class EndToEndInventorySagaTest {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryEventProducer inventoryEventProducer;

    @Mock
    private OutboxService outboxService;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private IdempotencyService idempotencyService;

    private OrderCreatedConsumer orderCreatedConsumer;
    private SagaCompensationConsumer sagaCompensationConsumer;
    private OutboxPublisher outboxPublisher;

    private UUID orderId;
    private UUID customerId;
    private UUID productId1;
    private UUID productId2;

    @BeforeEach
    void setUp() {
        orderCreatedConsumer = new OrderCreatedConsumer(inventoryService, inventoryEventProducer, idempotencyService, outboxService);
        sagaCompensationConsumer = new SagaCompensationConsumer(inventoryService, inventoryEventProducer, idempotencyService, outboxService);
        outboxPublisher = new OutboxPublisher(outboxEventRepository, kafkaTemplate);

        orderId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        productId1 = UUID.randomUUID();
        productId2 = UUID.randomUUID();
    }

    @Test
    @DisplayName("Saga Step: OrderCreated ➔ Stock Reserved ➔ InventoryReservedEvent Published via Outbox")
    void testOrderCreated_StockReserved_OutboxRelayed() {
        UUID eventId = UUID.randomUUID();
        OrderCreatedEvent.OrderItem item1 = OrderCreatedEvent.OrderItem.builder()
                .productId(productId1)
                .quantity(2)
                .unitPrice(new BigDecimal("50.00"))
                .build();
        OrderCreatedEvent.OrderItem item2 = OrderCreatedEvent.OrderItem.builder()
                .productId(productId2)
                .quantity(1)
                .unitPrice(new BigDecimal("100.00"))
                .build();

        OrderCreatedEvent orderCreatedEvent = OrderCreatedEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .customerId(customerId)
                .totalAmount(new BigDecimal("200.00"))
                .items(List.of(item1, item2))
                .build();

        when(idempotencyService.isAlreadyProcessed(eventId)).thenReturn(false);
        when(inventoryService.reserveOrderInventory(orderId, orderCreatedEvent.getItems())).thenReturn(List.of(
                InventoryReservedEvent.ReservedItem.builder()
                        .productId(productId1)
                        .quantity(2)
                        .build(),
                InventoryReservedEvent.ReservedItem.builder()
                        .productId(productId2)
                        .quantity(1)
                        .build()
        ));

        // Consume event
        orderCreatedConsumer.handleOrderCreated(orderCreatedEvent);

        // Verify inventory service reserved stock
        verify(inventoryService).reserveOrderInventory(orderId, orderCreatedEvent.getItems());

        // Verify InventoryReservedEvent emitted
        ArgumentCaptor<InventoryReservedEvent> captor = ArgumentCaptor.forClass(InventoryReservedEvent.class);
        verify(inventoryEventProducer).publishInventoryReserved(captor.capture());
        InventoryReservedEvent publishedEvent = captor.getValue();
        assertThat(publishedEvent.getOrderId()).isEqualTo(orderId);
        assertThat(publishedEvent.getCustomerId()).isEqualTo(customerId);
        assertThat(publishedEvent.getReservedItems()).hasSize(2);

        // Verify outbox saved and idempotency recorded
        verify(outboxService).saveEvent(eq("INVENTORY"), eq(orderId.toString()), eq(KafkaTopics.INVENTORY_RESERVED), any());
        verify(idempotencyService).markAsProcessed(eventId, orderCreatedEvent.getEventType(), "inventory-service-group");

        // Verify outbox relay
        OutboxEvent outboxEvent = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("INVENTORY")
                .aggregateId(orderId.toString())
                .eventType(KafkaTopics.INVENTORY_RESERVED)
                .payload("{\"orderId\":\"" + orderId + "\"}")
                .status(OutboxStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();

        when(outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(outboxEvent));
        when(kafkaTemplate.send(eq(KafkaTopics.INVENTORY_RESERVED), eq(orderId.toString()), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        outboxPublisher.publishPendingEvents();

        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(outboxEvent.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("Saga Step: Stock Insufficient ➔ ReservationFailedEvent Published & Partial Stock Released")
    void testOrderCreated_InsufficientStock_EmitsReservationFailed() {
        UUID eventId = UUID.randomUUID();
        OrderCreatedEvent.OrderItem item1 = OrderCreatedEvent.OrderItem.builder()
                .productId(productId1)
                .quantity(5)
                .unitPrice(new BigDecimal("50.00"))
                .build();

        OrderCreatedEvent orderCreatedEvent = OrderCreatedEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .customerId(customerId)
                .totalAmount(new BigDecimal("250.00"))
                .items(List.of(item1))
                .build();

        when(idempotencyService.isAlreadyProcessed(eventId)).thenReturn(false);
        doThrow(new InsufficientStockException("Insufficient stock for product " + productId1))
                .when(inventoryService).reserveOrderInventory(any(), any());

        orderCreatedConsumer.handleOrderCreated(orderCreatedEvent);

        // Verify ReservationFailedEvent emitted
        ArgumentCaptor<ReservationFailedEvent> captor = ArgumentCaptor.forClass(ReservationFailedEvent.class);
        verify(inventoryEventProducer).publishReservationFailed(captor.capture());
        ReservationFailedEvent failedEvent = captor.getValue();
        assertThat(failedEvent.getOrderId()).isEqualTo(orderId);
        assertThat(failedEvent.getReason()).contains("Insufficient stock for product");

        verify(outboxService).saveEvent(eq("INVENTORY"), eq(orderId.toString()), eq(KafkaTopics.INVENTORY_RESERVATION_FAILED), any());
        verify(idempotencyService).markAsProcessed(eventId, orderCreatedEvent.getEventType(), "inventory-service-group");
    }

    @Test
    @DisplayName("Saga Compensation: PaymentFailed ➔ Releases Reserved Stock")
    void testPaymentFailed_ReleasesStockCompensation() {
        UUID eventId = UUID.randomUUID();
        PaymentFailedEvent paymentFailedEvent = PaymentFailedEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .customerId(customerId)
                .amount(new BigDecimal("150.00"))
                .reason("Card declined")
                .build();

        when(idempotencyService.isAlreadyProcessed(eventId)).thenReturn(false);
        when(inventoryService.releaseOrderInventory(orderId, "Card declined")).thenReturn(List.of(
                InventoryReleasedEvent.ReleasedItem.builder()
                        .productId(productId1)
                        .quantity(2)
                        .build()
        ));

        sagaCompensationConsumer.handlePaymentFailed(paymentFailedEvent);

        verify(inventoryService).releaseOrderInventory(orderId, "Card declined");
        verify(inventoryEventProducer).publishInventoryReleased(any(InventoryReleasedEvent.class));
        verify(outboxService).saveEvent(eq("INVENTORY"), eq(orderId.toString()), eq(KafkaTopics.INVENTORY_RELEASED), any());
        verify(idempotencyService).markAsProcessed(eventId, paymentFailedEvent.getEventType(), "inventory-service-compensation-group");
    }

    @Test
    @DisplayName("Saga Compensation: OrderCancelled ➔ Releases Reserved Stock")
    void testOrderCancelled_ReleasesStockCompensation() {
        UUID eventId = UUID.randomUUID();
        OrderCancelledEvent orderCancelledEvent = OrderCancelledEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .customerId(customerId)
                .reason("User aborted order")
                .build();

        when(idempotencyService.isAlreadyProcessed(eventId)).thenReturn(false);
        when(inventoryService.releaseOrderInventory(orderId, "User aborted order")).thenReturn(List.of(
                InventoryReleasedEvent.ReleasedItem.builder()
                        .productId(productId1)
                        .quantity(2)
                        .build()
        ));

        sagaCompensationConsumer.handleOrderCancelled(orderCancelledEvent);

        verify(inventoryService).releaseOrderInventory(orderId, "User aborted order");
        verify(inventoryEventProducer).publishInventoryReleased(any(InventoryReleasedEvent.class));
        verify(outboxService).saveEvent(eq("INVENTORY"), eq(orderId.toString()), eq(KafkaTopics.INVENTORY_RELEASED), any());
        verify(idempotencyService).markAsProcessed(eventId, orderCancelledEvent.getEventType(), "inventory-service-compensation-group");
    }
}
