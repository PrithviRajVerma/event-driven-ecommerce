package com.eventdriven.inventory.controller;

import com.eventdriven.inventory.dto.*;
import com.eventdriven.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public InventoryResponse createInventory(
            @Valid @RequestBody CreateInventoryRequest request
    ) {
        return inventoryService.createInventory(request);
    }

    @GetMapping("/products/{productId}")
    public InventoryResponse getInventoryByProductId(
            @PathVariable UUID productId
    ) {
        return inventoryService.getInventoryByProductId(productId);
    }

    @PostMapping("/reserve")
    public InventoryResponse reserve(
            @Valid @RequestBody ReserveInventoryRequest request
    ) {
        return inventoryService.reserveInventory(request);
    }

    @PostMapping("/release")
    public InventoryResponse release(
            @Valid @RequestBody ReleaseInventoryRequest request
    ) {
        return inventoryService.releaseInventory(request);
    }

    @PostMapping("/confirm")
    public InventoryResponse confirm(
            @Valid @RequestBody ConfirmInventoryRequest request
    ) {
        return inventoryService.confirmInventory(request);
    }

    @PatchMapping("/products/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public InventoryResponse updateStock(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateStockRequest request
    ) {
        return inventoryService.updateStock(productId, request);
    }

    @PostMapping("/products/{productId}/add-stock")
    @PreAuthorize("hasRole('ADMIN')")
    public InventoryResponse addStock(
            @PathVariable UUID productId,
            @Valid @RequestBody AddStockRequest request
    ) {
        return inventoryService.addStock(productId, request);
    }
}