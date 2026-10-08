package com.eventdriven.product.controller;

import com.eventdriven.product.dto.CreateProductRequest;
import com.eventdriven.product.dto.PatchProductRequest;
import com.eventdriven.product.dto.ProductResponse;
import com.eventdriven.product.dto.UpdateProductRequest;
import com.eventdriven.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Products", description = "Administrative endpoints for product lifecycle, creation, updates, and soft deletes")
public class AdminProductController {

    private final ProductService productService;

    @PostMapping
    @Operation(summary = "Create product", description = "Creates a new product in the catalog (ADMIN only).")
    public ProductResponse createProduct(@Valid @RequestBody CreateProductRequest request) {
        return productService.createProduct(request);
    }

    @GetMapping
    @Operation(summary = "Get all products", description = "Retrieves all products regardless of active status (ADMIN only).")
    public List<ProductResponse> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Retrieves full product details including inactive items (ADMIN only).")
    public ProductResponse getProduct(@PathVariable UUID id) {
        return productService.getProduct(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product", description = "Performs a full replacement update of a product (ADMIN only).")
    public ProductResponse updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        return productService.updateProduct(id, request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Patch product", description = "Performs a partial field update on a product (ADMIN only).")
    public ProductResponse patchProduct(
            @PathVariable UUID id,
            @Valid @RequestBody PatchProductRequest request
    ) {
        return productService.patchProduct(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product", description = "Soft-deletes a product by setting active=false (ADMIN only).")
    public void deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
    }
}