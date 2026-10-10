package com.eventdriven.order.controller;

import com.eventdriven.order.dto.CancelOrderRequest;
import com.eventdriven.order.dto.CheckoutRequest;
import com.eventdriven.order.dto.OrderResponse;
import com.eventdriven.order.exception.UnauthorizedCartAccessException;
import com.eventdriven.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Customer Order checkout, tracking, and lifecycle management")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    @Operation(
            summary = "Checkout active cart into an order",
            description = "Converts the authenticated customer's current cart items into a PENDING order, empties the cart, and broadcasts an OrderCreatedEvent to Kafka."
    )
    public ResponseEntity<OrderResponse> checkout(
            Authentication authentication,
            @Valid @RequestBody(required = false) CheckoutRequest request
    ) {
        UUID customerId = extractCustomerId(authentication);
        if (customerId == null) {
            throw new UnauthorizedCartAccessException("Authentication required to checkout");
        }
        OrderResponse response = orderService.createOrderFromCart(
                customerId,
                request != null ? request : new CheckoutRequest()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(
            summary = "List customer orders",
            description = "Retrieves the complete order history for the authenticated customer ordered by creation date descending."
    )
    public ResponseEntity<List<OrderResponse>> getOrders(Authentication authentication) {
        UUID customerId = extractCustomerId(authentication);
        if (customerId == null) {
            throw new UnauthorizedCartAccessException("Authentication required to view orders");
        }
        List<OrderResponse> orders = orderService.getCustomerOrders(customerId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    @Operation(
            summary = "Get order details",
            description = "Retrieves details and line items for a specific order belonging to the authenticated customer."
    )
    public ResponseEntity<OrderResponse> getOrder(
            Authentication authentication,
            @Parameter(description = "Order ID to look up")
            @PathVariable UUID orderId
    ) {
        UUID customerId = extractCustomerId(authentication);
        if (customerId == null) {
            throw new UnauthorizedCartAccessException("Authentication required to view order details");
        }
        OrderResponse response = orderService.getOrder(orderId, customerId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(
            summary = "Cancel an order",
            description = "Cancels an existing order in PENDING or CONFIRMED state and broadcasts an OrderCancelledEvent to Kafka."
    )
    public ResponseEntity<OrderResponse> cancelOrder(
            Authentication authentication,
            @Parameter(description = "Order ID to cancel")
            @PathVariable UUID orderId,
            @Valid @RequestBody(required = false) CancelOrderRequest request
    ) {
        UUID customerId = extractCustomerId(authentication);
        if (customerId == null) {
            throw new UnauthorizedCartAccessException("Authentication required to cancel order");
        }
        String reason = (request != null) ? request.getReason() : "Customer requested cancellation";
        OrderResponse response = orderService.cancelOrder(orderId, customerId, reason);
        return ResponseEntity.ok(response);
    }

    private UUID extractCustomerId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UUID userId) {
            return userId;
        }
        return null;
    }
}
