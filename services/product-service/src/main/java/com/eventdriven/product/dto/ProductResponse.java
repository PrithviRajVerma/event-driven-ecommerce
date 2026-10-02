package com.eventdriven.product.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        String currency,
        String imageUrl,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}