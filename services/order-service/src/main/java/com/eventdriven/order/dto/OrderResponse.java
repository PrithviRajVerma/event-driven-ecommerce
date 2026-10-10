package com.eventdriven.order.dto;

import com.eventdriven.order.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    @Schema(description = "Order ID", example = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d")
    private UUID id;

    @Schema(description = "Customer ID", example = "c1d2e3f4-a5b6-7c8d-9e0f-1a2b3c4d5e6f")
    private UUID customerId;

    @Schema(description = "Order status", example = "PENDING")
    private OrderStatus status;

    @Schema(description = "Total monetary amount for the order", example = "129.50")
    private BigDecimal totalAmount;

    @Schema(description = "Currency code", example = "USD")
    private String currency;

    @Schema(description = "Customer delivery/shipping address", example = "123 Market St, San Francisco, CA 94103")
    private String shippingAddress;

    @Schema(description = "List of items in the order")
    private List<OrderItemResponse> items;

    @Schema(description = "Order placement timestamp")
    private OffsetDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private OffsetDateTime updatedAt;
}
