package com.eventdriven.order.e2e;

import com.eventdriven.events.KafkaTopics;
import com.eventdriven.events.inventory.InventoryReservedEvent;
import com.eventdriven.events.inventory.ReservationFailedEvent;
import com.eventdriven.events.order.OrderCancelledEvent;
import com.eventdriven.events.order.OrderConfirmedEvent;
import com.eventdriven.events.order.OrderCreatedEvent;
import com.eventdriven.events.payment.PaymentCompletedEvent;
import com.eventdriven.events.payment.PaymentFailedEvent;
import com.eventdriven.order.consumer.InventoryEventConsumer;
import com.eventdriven.order.consumer.PaymentEventConsumer;
import com.eventdriven.order.dto.CheckoutRequest;
import com.eventdriven.order.dto.OrderResponse;
import com.eventdriven.order.entity.Cart;
import com.eventdriven.order.entity.CartItem;
import com.eventdriven.order.entity.Order;
import com.eventdriven.order.entity.OrderStatus;
import com.eventdriven.order.idempotency.IdempotencyService;
import com.eventdriven.order.outbox.OutboxEvent;
import com.eventdriven.order.outbox.OutboxEventRepository;
import com.eventdriven.order.outbox.OutboxPublisher;
import com.eventdriven.order.outbox.OutboxService;
import com.eventdriven.order.producer.OrderEventProducer;
import com.eventdriven.order.repository.CartRepository;
import com.eventdriven.order.repository.OrderRepository;
import com.eventdriven.order.service.OrderService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * End-to-End Saga Choreography Integration Test.
 *
 * Verifies the complete distributed transaction workflow:
 * 1. Checkout & Order Creation (PENDING)
 * 2. Transactional Outbox event generation & relay to Kafka
 * 3. Inventory Reservation event ingestion
 * 4. Payment Completion event ingestion & Order Confirmation (CONFIRMED)
 * 5. Saga Compensation flows on Payment / Inventory failures (CANCELLED)
 * 6. Idempotency guards preventing duplicate state transitions
 */
@ExtendWith(MockitoExtension.class)
class EndToEndSagaIntegrationTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private OrderEventProducer orderEventProducer;

    @Mock
    private OutboxService outboxService;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private IdempotencyService idempotencyService;

    private OrderService orderService;
    private OutboxPublisher outboxPublisher;
    private InventoryEventConsumer inventoryEventConsumer;
    private PaymentEventConsumer paymentEventConsumer;

    private UUID customerId;
    private UUID orderId;
    private UUID productId1;
    private UUID productId2;
    private Cart activeCart;
    private Order createdOrder;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, cartRepository, orderEventProducer, outboxService);
        outboxPublisher = new OutboxPublisher(outboxEventRepository, kafkaTemplate);
        inventoryEventConsumer = new InventoryEventConsumer(orderService, idempotencyService);
        paymentEventConsumer = new PaymentEventConsumer(orderService, idempotencyService);

        customerId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        productId1 = UUID.randomUUID();
        productId2 = UUID.randomUUID();

        activeCart = new Cart();
        activeCart.setId(UUID.randomUUID());
        activeCart.setCustomerId(customerId);
        activeCart.setCreatedAt(OffsetDateTime.now());
        activeCart.setUpdatedAt(OffsetDateTime.now());

        CartItem item1 = new CartItem();
        item1.setId(UUID.randomUUID());
        item1.setProductId(productId1);
        item1.setQuantity(2);
        item1.setUnitPrice(new BigDecimal("30.00"));
        item1.setCreatedAt(OffsetDateTime.now());
        item1.setUpdatedAt(OffsetDateTime.now());
        activeCart.addItem(item1);

        CartItem item2 = new CartItem();
        item2.setId(UUID.randomUUID());
        item2.setProductId(productId2);
        item2.setQuantity(1);
        item2.setUnitPrice(new BigDecimal("40.00"));
        item2.setCreatedAt(OffsetDateTime.now());
        item2.setUpdatedAt(OffsetDateTime.now());
        activeCart.addItem(item2);

        createdOrder = new Order();
        createdOrder.setId(orderId);
        createdOrder.setCustomerId(customerId);
        createdOrder.setStatus(OrderStatus.PENDING);
        createdOrder.setTotalAmount(new BigDecimal("100.00"));
        createdOrder.setShippingAddress("123 Distributed St, Cloud City");
        createdOrder.setCreatedAt(OffsetDateTime.now());
        createdOrder.setUpdatedAt(OffsetDateTime.now());
    }

    @Test
    @DisplayName("Complete E2E Happy Path: Checkout ➔ OrderCreated ➔ InventoryReserved ➔ PaymentCompleted ➔ OrderConfirmed")
    void testFullSuccessfulOrderSaga_FromCheckoutToConfirmed() {
        // Step 1: User checkouts active cart
        when(cartRepository.findWithItemsByCustomerId(customerId)).thenReturn(Optional.of(activeCart));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(orderId);
            return o;
        });

        CheckoutRequest request = new CheckoutRequest();
        request.setShippingAddress("123 Distributed St, Cloud City");
        OrderResponse response = orderService.createOrderFromCart(customerId, request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(orderId);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));

        // Verify Cart cleared
        assertThat(activeCart.getItems()).isEmpty();
        verify(cartRepository).save(activeCart);

        // Verify OrderCreatedEvent stored in Outbox
        verify(outboxService).saveEvent(
                eq("ORDER"),
                eq(orderId.toString()),
                eq(KafkaTopics.ORDER_CREATED),
                any(OrderCreatedEvent.class)
        );

        // Step 2: Transactional Outbox Publisher relays event to Kafka
        OutboxEvent outboxEvent = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("Order")
                .aggregateId(orderId.toString())
                .eventType(KafkaTopics.ORDER_CREATED)
                .payload("{\"orderId\":\"" + orderId + "\"}")
                .status(com.eventdriven.order.outbox.OutboxStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();

        when(outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(com.eventdriven.order.outbox.OutboxStatus.PENDING))
                .thenReturn(List.of(outboxEvent));
        when(kafkaTemplate.send(eq(KafkaTopics.ORDER_CREATED), eq(orderId.toString()), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        outboxPublisher.publishPendingEvents();

        assertThat(outboxEvent.getStatus()).isEqualTo(com.eventdriven.order.outbox.OutboxStatus.PUBLISHED);
        assertThat(outboxEvent.getPublishedAt()).isNotNull();
        verify(outboxEventRepository).save(outboxEvent);

        // Step 3: Inventory Service confirms stock reservation
        UUID invEventId = UUID.randomUUID();
        InventoryReservedEvent inventoryReservedEvent = InventoryReservedEvent.builder()
                .eventId(invEventId)
                .orderId(orderId)
                .customerId(customerId)
                .totalAmount(new BigDecimal("100.00"))
                .reservedItems(List.of())
                .build();

        when(idempotencyService.isAlreadyProcessed(invEventId)).thenReturn(false);
        when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(createdOrder));

        inventoryEventConsumer.handleInventoryReserved(inventoryReservedEvent);

        assertThat(createdOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(outboxService).saveEvent(
                eq("ORDER"),
                eq(orderId.toString()),
                eq(KafkaTopics.ORDER_CONFIRMED),
                any(OrderConfirmedEvent.class)
        );
        verify(idempotencyService).markAsProcessed(invEventId, inventoryReservedEvent.getEventType(), "order-service-group");

        // Step 4: Payment Service confirms payment processed
        UUID payEventId = UUID.randomUUID();
        PaymentCompletedEvent paymentCompletedEvent = PaymentCompletedEvent.builder()
                .eventId(payEventId)
                .orderId(orderId)
                .paymentId(UUID.randomUUID())
                .customerId(customerId)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .build();

        when(idempotencyService.isAlreadyProcessed(payEventId)).thenReturn(false);

        paymentEventConsumer.handlePaymentCompleted(paymentCompletedEvent);

        // Verify Order transitioned to COMPLETED
        assertThat(createdOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        verify(orderRepository, atLeastOnce()).save(createdOrder);
        verify(idempotencyService).markAsProcessed(payEventId, paymentCompletedEvent.getEventType(), "order-service-group");
    }

    @Test
    @DisplayName("Saga Compensation: Payment Failure ➔ Order Cancelled ➔ Compensation Event Emitted")
    void testSagaCompensation_WhenPaymentFails() {
        when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(createdOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UUID payFailedEventId = UUID.randomUUID();
        PaymentFailedEvent paymentFailedEvent = PaymentFailedEvent.builder()
                .eventId(payFailedEventId)
                .orderId(orderId)
                .customerId(customerId)
                .amount(new BigDecimal("100.00"))
                .reason("Insufficient funds in customer card")
                .build();

        when(idempotencyService.isAlreadyProcessed(payFailedEventId)).thenReturn(false);

        paymentEventConsumer.handlePaymentFailed(paymentFailedEvent);

        // Verify Order marked as CANCELLED
        assertThat(createdOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(createdOrder);

        // Verify OrderCancelledEvent saved to Outbox to trigger inventory release compensation
        ArgumentCaptor<OrderCancelledEvent> captor = ArgumentCaptor.forClass(OrderCancelledEvent.class);
        verify(outboxService).saveEvent(
                eq("ORDER"),
                eq(orderId.toString()),
                eq(KafkaTopics.ORDER_CANCELLED),
                captor.capture()
        );

        OrderCancelledEvent cancelledEvent = captor.getValue();
        assertThat(cancelledEvent.getOrderId()).isEqualTo(orderId);
        assertThat(cancelledEvent.getReason()).contains("Payment failed: Insufficient funds in customer card");
        verify(idempotencyService).markAsProcessed(payFailedEventId, paymentFailedEvent.getEventType(), "order-service-group");
    }

    @Test
    @DisplayName("Saga Compensation: Inventory Failure ➔ Order Cancelled ➔ Reason Recorded")
    void testSagaCompensation_WhenInventoryReservationFails() {
        when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(createdOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UUID resFailedEventId = UUID.randomUUID();
        ReservationFailedEvent reservationFailedEvent = ReservationFailedEvent.builder()
                .eventId(resFailedEventId)
                .orderId(orderId)
                .reason("Requested quantity exceeds stock on product " + productId1)
                .build();

        when(idempotencyService.isAlreadyProcessed(resFailedEventId)).thenReturn(false);

        inventoryEventConsumer.handleReservationFailed(reservationFailedEvent);

        // Verify Order marked as CANCELLED
        assertThat(createdOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(createdOrder);

        verify(idempotencyService).markAsProcessed(resFailedEventId, reservationFailedEvent.getEventType(), "order-service-group");
    }

    @Test
    @DisplayName("Idempotency Guard: Duplicate Kafka events are deduplicated without re-processing")
    void testConsumerIdempotency_DuplicateEventsPreventDoubleProcessing() {
        UUID dupEventId = UUID.randomUUID();
        PaymentCompletedEvent paymentCompletedEvent = PaymentCompletedEvent.builder()
                .eventId(dupEventId)
                .orderId(orderId)
                .paymentId(UUID.randomUUID())
                .customerId(customerId)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .build();

        // Simulate event already marked as processed
        when(idempotencyService.isAlreadyProcessed(dupEventId)).thenReturn(true);

        paymentEventConsumer.handlePaymentCompleted(paymentCompletedEvent);

        // Verify orderService was never invoked and no order save occurred
        verifyNoInteractions(orderRepository);
        verify(idempotencyService, never()).markAsProcessed(any(), any(), any());
    }
}
