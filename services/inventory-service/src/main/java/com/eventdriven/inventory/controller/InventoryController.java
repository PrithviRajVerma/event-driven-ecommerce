package com.eventdriven.inventory.controller;

import com.eventdriven.inventory.dto.*;
import com.eventdriven.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Stock tracking, optimistic reservation, release, and admin stock management")
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Initialize product inventory", description = "Creates an inventory record with initial stock for a product (ADMIN only).")
    public InventoryResponse createInventory(@Valid @RequestBody CreateInventoryRequest request) {
        return inventoryService.createInventory(request);
    }

    @GetMapping("/products/{productId}")
    @Operation(summary = "Get inventory by product ID", description = "Retrieves current available and reserved stock for a product.")
    public InventoryResponse getInventoryByProductId(@PathVariable UUID productId) {
        return inventoryService.getInventoryByProductId(productId);
    }

    @PostMapping("/reserve")
    @Operation(summary = "Reserve stock", description = "Reserves requested quantity for an order using optimistic locking (@Version).")
    public InventoryResponse reserve(@Valid @RequestBody ReserveInventoryRequest request) {
        return inventoryService.reserveInventory(request);
    }

    @PostMapping("/release")
    @Operation(summary = "Release reserved stock", description = "Rolls back a stock reservation when order checkout fails or is cancelled.")
    public InventoryResponse release(@Valid @RequestBody ReleaseInventoryRequest request) {
        return inventoryService.releaseInventory(request);
    }

    @PostMapping("/confirm")
    @Operation(summary = "Confirm stock reservation", description = "Confirms and finalizes stock deduction upon payment success.")
    public InventoryResponse confirm(@Valid @RequestBody ConfirmInventoryRequest request) {
        return inventoryService.confirmInventory(request);
    }

    @PatchMapping("/products/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Set stock quantity", description = "Updates the exact available stock quantity for a product (ADMIN only).")
    public InventoryResponse updateStock(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateStockRequest request
    ) {
        return inventoryService.updateStock(productId, request);
    }

    @PostMapping("/products/{productId}/add-stock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Replenish stock", description = "Adds additional stock units to existing product inventory (ADMIN only).")
    public InventoryResponse addStock(
            @PathVariable UUID productId,
            @Valid @RequestBody AddStockRequest request
    ) {
        return inventoryService.addStock(productId, request);
    }
}