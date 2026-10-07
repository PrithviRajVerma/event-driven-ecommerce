package com.eventdriven.inventory.service;

import com.eventdriven.inventory.dto.*;
import com.eventdriven.inventory.entity.Inventory;
import com.eventdriven.inventory.exception.InsufficientReservedStockException;
import com.eventdriven.inventory.exception.InsufficientStockException;
import com.eventdriven.inventory.exception.InventoryAlreadyExistsException;
import com.eventdriven.inventory.exception.InventoryNotFoundException;
import com.eventdriven.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

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

    @Transactional
    public InventoryResponse reserveInventory(ReserveInventoryRequest request) {

        Inventory inventory = inventoryRepository
                .findByProductId(request.productId())
                .orElseThrow(InventoryNotFoundException::new);

        if (inventory.getAvailableQuantity() < request.quantity()) {
            throw new InsufficientStockException();
        }

        inventory.setAvailableQuantity(
                inventory.getAvailableQuantity() - request.quantity()
        );

        inventory.setReservedQuantity(
                inventory.getReservedQuantity() + request.quantity()
        );

        inventory.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        return toResponse(inventoryRepository.save(inventory));
    }

    @Transactional
    public InventoryResponse releaseInventory(ReleaseInventoryRequest request) {

        Inventory inventory = inventoryRepository
                .findByProductId(request.productId())
                .orElseThrow(InventoryNotFoundException::new);

        if (inventory.getReservedQuantity() < request.quantity()) {
            throw new InsufficientReservedStockException();
        }

        inventory.setAvailableQuantity(
                inventory.getAvailableQuantity() + request.quantity()
        );

        inventory.setReservedQuantity(
                inventory.getReservedQuantity() - request.quantity()
        );

        inventory.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        return toResponse(inventoryRepository.save(inventory));
    }

    @Transactional
    public InventoryResponse confirmInventory(ConfirmInventoryRequest request) {

        Inventory inventory = inventoryRepository
                .findByProductId(request.productId())
                .orElseThrow(InventoryNotFoundException::new);

        if (inventory.getReservedQuantity() < request.quantity()) {
            throw new InsufficientReservedStockException();
        }

        inventory.setReservedQuantity(
                inventory.getReservedQuantity() - request.quantity()
        );

        inventory.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        return toResponse(inventoryRepository.save(inventory));
    }

    @Transactional
    public InventoryResponse addStock(UUID productId, AddStockRequest request) {

        Inventory inventory = inventoryRepository
                .findByProductId(productId)
                .orElseThrow(InventoryNotFoundException::new);

        inventory.setAvailableQuantity(
                inventory.getAvailableQuantity() + request.quantity()
        );

        inventory.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        return toResponse(inventoryRepository.save(inventory));
    }

    @Transactional
    public InventoryResponse updateStock(UUID productId, UpdateStockRequest request) {

        Inventory inventory = inventoryRepository
                .findByProductId(productId)
                .orElseThrow(InventoryNotFoundException::new);

        inventory.setAvailableQuantity(request.availableQuantity());

        inventory.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        return toResponse(inventoryRepository.save(inventory));
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