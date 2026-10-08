package com.eventdriven.order.redis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuestCart {
    private String guestCartId;

    @Builder.Default
    private List<GuestCartItem> items = new ArrayList<>();

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Optional<GuestCartItem> findItemByProductId(UUID productId) {
        if (items == null) {
            return Optional.empty();
        }
        return items.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();
    }
}
