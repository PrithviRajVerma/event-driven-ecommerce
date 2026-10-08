package com.eventdriven.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistResponse {
    private UUID customerId;
    private List<WishlistItemResponse> items;
    private Integer totalItems;
    private OffsetDateTime updatedAt;
}
