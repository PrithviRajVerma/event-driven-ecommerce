package com.eventdriven.order.dto;

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
public class CartResponse {
    private String cartId;
    private UUID customerId;
    private List<CartItemResponse> items;
    private Integer totalItems;
    private BigDecimal totalAmount;
    private OffsetDateTime updatedAt;
}
