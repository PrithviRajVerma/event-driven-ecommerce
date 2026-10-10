package com.eventdriven.order.service;

import com.eventdriven.events.order.OrderCancelledEvent;
import com.eventdriven.events.order.OrderConfirmedEvent;
import com.eventdriven.events.order.OrderCreatedEvent;
import com.eventdriven.order.dto.*;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final OrderEventProducer orderEventProducer;

    @Transactional
    public OrderResponse createOrderFromCart(UUID customerId, CheckoutRequest request) {
        if (customerId == null) {
            throw new UnauthorizedCartAccessException("Authentication required to checkout");
        }

        Cart cart = cartRepository.findWithItemsByCustomerId(customerId)
                .orElseThrow(() -> new InvalidOrderOperationException("Cart is empty or not found. Cannot proceed to checkout."));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new InvalidOrderOperationException("Cannot checkout with an empty cart");
        }

        OffsetDateTime now = OffsetDateTime.now();

        Order order = new Order();
        order.setCustomerId(customerId);
        order.setStatus(OrderStatus.PENDING);
        order.setCurrency("USD");
        order.setShippingAddress(request != null ? request.getShippingAddress() : null);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderCreatedEvent.OrderItem> eventItems = new ArrayList<>();

        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getUnitPrice());

            BigDecimal subtotal = cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            orderItem.setSubtotal(subtotal);
            orderItem.setCreatedAt(now);

            order.addItem(orderItem);
            totalAmount = totalAmount.add(subtotal);

            eventItems.add(OrderCreatedEvent.OrderItem.builder()
                    .productId(cartItem.getProductId())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(cartItem.getUnitPrice())
                    .build());
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

        // Clear customer cart after successful order creation
        cart.getItems().clear();
        cart.setUpdatedAt(now);
        cartRepository.save(cart);
        log.info("Cleared active cart for customer {} after creating order {}", customerId, savedOrder.getId());

        // Publish OrderCreatedEvent to Kafka
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(savedOrder.getId())
                .customerId(savedOrder.getCustomerId())
                .totalAmount(savedOrder.getTotalAmount())
                .items(eventItems)
                .build();

        orderEventProducer.publishOrderCreated(event);
        log.info("Created order {} for customer {} with total amount {}", savedOrder.getId(), customerId, totalAmount);

        return mapToOrderResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId, UUID customerId) {
        if (customerId == null) {
            throw new UnauthorizedCartAccessException("Authentication required to view order");
        }

        Order order = orderRepository.findWithItemsByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        return mapToOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getCustomerOrders(UUID customerId) {
        if (customerId == null) {
            throw new UnauthorizedCartAccessException("Authentication required to view orders");
        }

        List<Order> orders = orderRepository.findWithItemsByCustomerIdOrderByCreatedAtDesc(customerId);
        return orders.stream()
                .map(this::mapToOrderResponse)
                .toList();
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId, UUID customerId, String reason) {
        if (customerId == null) {
            throw new UnauthorizedCartAccessException("Authentication required to cancel order");
        }

        Order order = orderRepository.findWithItemsByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.canBeCancelled()) {
            throw new InvalidOrderOperationException("Order with status " + order.getStatus() + " cannot be cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(OffsetDateTime.now());
        Order savedOrder = orderRepository.save(order);

        String cancellationReason = (reason != null && !reason.isBlank()) ? reason : "Customer requested cancellation";

        // Publish OrderCancelledEvent to Kafka
        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(savedOrder.getId())
                .customerId(savedOrder.getCustomerId())
                .reason(cancellationReason)
                .build();

        orderEventProducer.publishOrderCancelled(event);
        log.info("Cancelled order {} for customer {}. Reason: {}", orderId, customerId, cancellationReason);

        return mapToOrderResponse(savedOrder);
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(newStatus);
        order.setUpdatedAt(OffsetDateTime.now());
        Order savedOrder = orderRepository.save(order);

        log.info("Updated order {} status from {} to {}", orderId, previousStatus, newStatus);

        if (newStatus == OrderStatus.CONFIRMED) {
            orderEventProducer.publishOrderConfirmed(OrderConfirmedEvent.builder()
                    .orderId(savedOrder.getId())
                    .customerId(savedOrder.getCustomerId())
                    .build());
        } else if (newStatus == OrderStatus.CANCELLED) {
            orderEventProducer.publishOrderCancelled(OrderCancelledEvent.builder()
                    .orderId(savedOrder.getId())
                    .customerId(savedOrder.getCustomerId())
                    .reason("Status updated to CANCELLED")
                    .build());
        }

        return mapToOrderResponse(savedOrder);
    }

    @Transactional
    public void handleInventoryReserved(UUID orderId) {
        if (orderId == null) {
            log.warn("Cannot handle InventoryReservedEvent with null orderId");
            return;
        }

        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
            order.setUpdatedAt(OffsetDateTime.now());
            Order savedOrder = orderRepository.save(order);
            log.info("Order {} confirmed after inventory reserved successfully", orderId);

            orderEventProducer.publishOrderConfirmed(OrderConfirmedEvent.builder()
                    .orderId(savedOrder.getId())
                    .customerId(savedOrder.getCustomerId())
                    .build());
        } else {
            log.warn("Ignoring InventoryReservedEvent for order {} with status {}", orderId, order.getStatus());
        }
    }

    @Transactional
    public void handleInventoryReservationFailed(UUID orderId, String reason) {
        if (orderId == null) {
            log.warn("Cannot handle ReservationFailedEvent with null orderId");
            return;
        }

        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.canBeCancelled()) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setUpdatedAt(OffsetDateTime.now());
            Order savedOrder = orderRepository.save(order);

            String cancellationReason = (reason != null && !reason.isBlank())
                    ? "Inventory reservation failed: " + reason
                    : "Inventory reservation failed";
            log.info("Order {} cancelled due to inventory reservation failure. Reason: {}", orderId, cancellationReason);

            orderEventProducer.publishOrderCancelled(OrderCancelledEvent.builder()
                    .orderId(savedOrder.getId())
                    .customerId(savedOrder.getCustomerId())
                    .reason(cancellationReason)
                    .build());
        } else {
            log.warn("Cannot cancel order {} with status {} on inventory reservation failure", orderId, order.getStatus());
        }
    }

    @Transactional
    public void handlePaymentCompleted(UUID orderId) {
        if (orderId == null) {
            log.warn("Cannot handle PaymentCompletedEvent with null orderId");
            return;
        }

        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.COMPLETED);
            order.setUpdatedAt(OffsetDateTime.now());
            orderRepository.save(order);
            log.info("Order {} completed following successful payment", orderId);
        } else {
            log.warn("Ignoring PaymentCompletedEvent for order {} with status {}", orderId, order.getStatus());
        }
    }

    @Transactional
    public void handlePaymentFailed(UUID orderId, String reason) {
        if (orderId == null) {
            log.warn("Cannot handle PaymentFailedEvent with null orderId");
            return;
        }

        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.canBeCancelled()) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setUpdatedAt(OffsetDateTime.now());
            Order savedOrder = orderRepository.save(order);

            String cancellationReason = (reason != null && !reason.isBlank())
                    ? "Payment failed: " + reason
                    : "Payment failed";
            log.info("Order {} cancelled following payment failure. Reason: {}", orderId, cancellationReason);

            orderEventProducer.publishOrderCancelled(OrderCancelledEvent.builder()
                    .orderId(savedOrder.getId())
                    .customerId(savedOrder.getCustomerId())
                    .reason(cancellationReason)
                    .build());
        } else {
            log.warn("Cannot cancel order {} with status {} on payment failure", orderId, order.getStatus());
        }
    }

    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .shippingAddress(order.getShippingAddress())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
