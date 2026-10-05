package com.eventdriven.inventory.controller;

import com.eventdriven.inventory.dto.CreateInventoryRequest;
import com.eventdriven.inventory.dto.InventoryResponse;
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
}