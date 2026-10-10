package com.eventdriven.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {

    @Schema(description = "Order item ID", example = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d")
    private UUID id;

    @Schema(description = "Product ID", example = "b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e")
    private UUID productId;

    @Schema(description = "Quantity purchased", example = "2")
    private Integer quantity;

    @Schema(description = "Price per unit at checkout time", example = "29.99")
    private BigDecimal unitPrice;

    @Schema(description = "Subtotal for this line item", example = "59.98")
    private BigDecimal subtotal;
}
