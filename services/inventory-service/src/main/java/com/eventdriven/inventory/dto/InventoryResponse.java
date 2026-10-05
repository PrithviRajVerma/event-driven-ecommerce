package com.eventdriven.inventory.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InventoryResponse(
        UUID id,
        UUID productId,
        Integer availableQuantity,
        Integer reservedQuantity,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}