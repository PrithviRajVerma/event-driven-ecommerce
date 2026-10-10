package com.eventdriven.order.service;

import com.eventdriven.events.order.OrderCancelledEvent;
import com.eventdriven.events.order.OrderConfirmedEvent;
import com.eventdriven.events.order.OrderCreatedEvent;
import com.eventdriven.order.dto.CheckoutRequest;
import com.eventdriven.order.dto.OrderResponse;
import com.eventdriven.order.entity.Cart;
import com.eventdriven.order.entity.CartItem;
import com.eventdriven.order.entity.Order;
import com.eventdriven.order.entity.OrderItem;
import com.eventdriven.order.entity.OrderStatus;
import com.eventdriven.order.exception.InvalidOrderOperationException;
import com.eventdriven.order.exception.OrderNotFoundException;
import com.eventdriven.order.exception.UnauthorizedCartAccessException;
import com.eventdriven.order.producer.OrderEventProducer;
import com.eventdriven.order.repository.CartRepository;
import com.eventdriven.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private OrderEventProducer orderEventProducer;

    @InjectMocks
    private OrderService orderService;

    private UUID customerId;
    private UUID productId1;
    private UUID productId2;
    private Cart testCart;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        productId1 = UUID.randomUUID();
        productId2 = UUID.randomUUID();

        testCart = new Cart();
        testCart.setId(UUID.randomUUID());
        testCart.setCustomerId(customerId);
        testCart.setCreatedAt(OffsetDateTime.now());
        testCart.setUpdatedAt(OffsetDateTime.now());

        CartItem item1 = new CartItem();
        item1.setId(UUID.randomUUID());
        item1.setProductId(productId1);
        item1.setQuantity(2);
        item1.setUnitPrice(new BigDecimal("25.00"));
        item1.setCreatedAt(OffsetDateTime.now());
        item1.setUpdatedAt(OffsetDateTime.now());
        testCart.addItem(item1);

        CartItem item2 = new CartItem();
        item2.setId(UUID.randomUUID());
        item2.setProductId(productId2);
        item2.setQuantity(1);
        item2.setUnitPrice(new BigDecimal("50.00"));
        item2.setCreatedAt(OffsetDateTime.now());
        item2.setUpdatedAt(OffsetDateTime.now());
        testCart.addItem(item2);
    }

    @Test
    void createOrderFromCart_successfulCheckout() {
        when(cartRepository.findWithItemsByCustomerId(customerId)).thenReturn(Optional.of(testCart));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });

        CheckoutRequest request = CheckoutRequest.builder()
                .shippingAddress("123 Market St, San Francisco, CA")
                .build();

        OrderResponse response = orderService.createOrderFromCart(customerId, request);

        assertThat(response).isNotNull();
        assertThat(response.getCustomerId()).isEqualTo(customerId);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getShippingAddress()).isEqualTo("123 Market St, San Francisco, CA");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("100.00");
        assertThat(response.getItems()).hasSize(2);

        // Cart should have been cleared
        assertThat(testCart.getItems()).isEmpty();
        verify(cartRepository).save(testCart);

        // Verify Kafka event published
        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(orderEventProducer).publishOrderCreated(eventCaptor.capture());
        OrderCreatedEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getOrderId()).isEqualTo(response.getId());
        assertThat(publishedEvent.getCustomerId()).isEqualTo(customerId);
        assertThat(publishedEvent.getTotalAmount()).isEqualByComparingTo("100.00");
        assertThat(publishedEvent.getItems()).hasSize(2);
    }

    @Test
    void createOrderFromCart_unauthenticated_throwsUnauthorized() {
        CheckoutRequest request = CheckoutRequest.builder().build();

        assertThatThrownBy(() -> orderService.createOrderFromCart(null, request))
                .isInstanceOf(UnauthorizedCartAccessException.class)
                .hasMessageContaining("Authentication required");

        verifyNoInteractions(cartRepository);
        verifyNoInteractions(orderRepository);
        verifyNoInteractions(orderEventProducer);
    }

    @Test
    void createOrderFromCart_cartNotFound_throwsInvalidOrderOperation() {
        when(cartRepository.findWithItemsByCustomerId(customerId)).thenReturn(Optional.empty());

        CheckoutRequest request = CheckoutRequest.builder().build();

        assertThatThrownBy(() -> orderService.createOrderFromCart(customerId, request))
                .isInstanceOf(InvalidOrderOperationException.class)
                .hasMessageContaining("Cart is empty or not found");

        verifyNoInteractions(orderRepository);
        verifyNoInteractions(orderEventProducer);
    }

    @Test
    void createOrderFromCart_emptyCart_throwsInvalidOrderOperation() {
        testCart.getItems().clear();
        when(cartRepository.findWithItemsByCustomerId(customerId)).thenReturn(Optional.of(testCart));

        CheckoutRequest request = CheckoutRequest.builder().build();

        assertThatThrownBy(() -> orderService.createOrderFromCart(customerId, request))
                .isInstanceOf(InvalidOrderOperationException.class)
                .hasMessageContaining("Cannot checkout with an empty cart");

        verifyNoInteractions(orderRepository);
        verifyNoInteractions(orderEventProducer);
    }

    @Test
    void getOrder_successful() {
        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, customerId, OrderStatus.PENDING);

        when(orderRepository.findWithItemsByIdAndCustomerId(orderId, customerId)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrder(orderId, customerId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(orderId);
        assertThat(response.getCustomerId()).isEqualTo(customerId);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void getOrder_notFound_throwsOrderNotFound() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findWithItemsByIdAndCustomerId(orderId, customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(orderId, customerId))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void getCustomerOrders_returnsList() {
        UUID orderId1 = UUID.randomUUID();
        UUID orderId2 = UUID.randomUUID();
        Order order1 = createMockOrder(orderId1, customerId, OrderStatus.PENDING);
        Order order2 = createMockOrder(orderId2, customerId, OrderStatus.CONFIRMED);

        when(orderRepository.findWithItemsByCustomerIdOrderByCreatedAtDesc(customerId))
                .thenReturn(List.of(order1, order2));

        List<OrderResponse> orders = orderService.getCustomerOrders(customerId);

        assertThat(orders).hasSize(2);
        assertThat(orders.get(0).getId()).isEqualTo(orderId1);
        assertThat(orders.get(1).getId()).isEqualTo(orderId2);
    }

    @Test
    void cancelOrder_pendingOrder_successful() {
        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, customerId, OrderStatus.PENDING);

        when(orderRepository.findWithItemsByIdAndCustomerId(orderId, customerId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.cancelOrder(orderId, customerId, "Found better price");

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        ArgumentCaptor<OrderCancelledEvent> eventCaptor = ArgumentCaptor.forClass(OrderCancelledEvent.class);
        verify(orderEventProducer).publishOrderCancelled(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getOrderId()).isEqualTo(orderId);
        assertThat(eventCaptor.getValue().getReason()).isEqualTo("Found better price");
    }

    @Test
    void cancelOrder_confirmedOrder_successful() {
        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, customerId, OrderStatus.CONFIRMED);

        when(orderRepository.findWithItemsByIdAndCustomerId(orderId, customerId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.cancelOrder(orderId, customerId, null);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderEventProducer).publishOrderCancelled(any(OrderCancelledEvent.class));
    }

    @Test
    void cancelOrder_alreadyCompleted_throwsInvalidOrderOperation() {
        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, customerId, OrderStatus.COMPLETED);

        when(orderRepository.findWithItemsByIdAndCustomerId(orderId, customerId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(orderId, customerId, "Need refund"))
                .isInstanceOf(InvalidOrderOperationException.class)
                .hasMessageContaining("cannot be cancelled");

        verify(orderRepository, never()).save(any());
        verifyNoInteractions(orderEventProducer);
    }

    @Test
    void cancelOrder_alreadyCancelled_throwsInvalidOrderOperation() {
        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, customerId, OrderStatus.CANCELLED);

        when(orderRepository.findWithItemsByIdAndCustomerId(orderId, customerId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(orderId, customerId, "Need refund"))
                .isInstanceOf(InvalidOrderOperationException.class)
                .hasMessageContaining("cannot be cancelled");

        verify(orderRepository, never()).save(any());
        verifyNoInteractions(orderEventProducer);
    }

    @Test
    void updateOrderStatus_toConfirmed_publishesOrderConfirmedEvent() {
        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, customerId, OrderStatus.PENDING);

        when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.updateOrderStatus(orderId, OrderStatus.CONFIRMED);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        ArgumentCaptor<OrderConfirmedEvent> eventCaptor = ArgumentCaptor.forClass(OrderConfirmedEvent.class);
        verify(orderEventProducer).publishOrderConfirmed(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getOrderId()).isEqualTo(orderId);
    }

    @Test
    void updateOrderStatus_toCancelled_publishesOrderCancelledEvent() {
        UUID orderId = UUID.randomUUID();
        Order order = createMockOrder(orderId, customerId, OrderStatus.PENDING);

        when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderEventProducer).publishOrderCancelled(any(OrderCancelledEvent.class));
    }

    private Order createMockOrder(UUID orderId, UUID customerId, OrderStatus status) {
        Order order = new Order();
        order.setId(orderId);
        order.setCustomerId(customerId);
        order.setStatus(status);
        order.setCurrency("USD");
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setShippingAddress("123 Market St, San Francisco, CA");
        order.setCreatedAt(OffsetDateTime.now());
        order.setUpdatedAt(OffsetDateTime.now());

        OrderItem item = new OrderItem();
        item.setId(UUID.randomUUID());
        item.setProductId(productId1);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("50.00"));
        item.setSubtotal(new BigDecimal("100.00"));
        item.setCreatedAt(OffsetDateTime.now());
        order.addItem(item);

        return order;
    }
}
