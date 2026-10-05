package com.eventdriven.inventory.service;

import com.eventdriven.inventory.dto.CreateInventoryRequest;
import com.eventdriven.inventory.dto.InventoryResponse;
import com.eventdriven.inventory.entity.Inventory;
import com.eventdriven.inventory.exception.InventoryAlreadyExistsException;
import com.eventdriven.inventory.exception.InventoryNotFoundException;
import com.eventdriven.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional
    public InventoryResponse createInventory(CreateInventoryRequest request) {

        if (inventoryRepository.findByProductId(request.productId()).isPresent()) {
            throw new InventoryAlreadyExistsException();
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        Inventory inventory = new Inventory();
        inventory.setProductId(request.productId());
        inventory.setAvailableQuantity(request.availableQuantity());
        inventory.setReservedQuantity(0);
        inventory.setCreatedAt(now);
        inventory.setUpdatedAt(now);

        Inventory savedInventory = inventoryRepository.save(inventory);

        return toResponse(savedInventory);
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByProductId(
            java.util.UUID productId
    ) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(InventoryNotFoundException::new);

        return toResponse(inventory);
    }

    private InventoryResponse toResponse(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getProductId(),
                inventory.getAvailableQuantity(),
                inventory.getReservedQuantity(),
                inventory.getCreatedAt(),
                inventory.getUpdatedAt()
        );
    }
}