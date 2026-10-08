package com.eventdriven.order.redis.model;

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
public class GuestCartItem {
    private UUID productId;
    private Integer quantity;
    private BigDecimal unitPrice;
}
