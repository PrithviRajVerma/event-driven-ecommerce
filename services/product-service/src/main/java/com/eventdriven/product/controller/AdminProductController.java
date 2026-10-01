package com.eventdriven.product.controller;

import com.eventdriven.product.dto.CreateProductRequest;
import com.eventdriven.product.dto.PatchProductRequest;
import com.eventdriven.product.dto.ProductResponse;
import com.eventdriven.product.dto.UpdateProductRequest;
import com.eventdriven.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    @PostMapping
    public ProductResponse createProduct(
            @Valid @RequestBody CreateProductRequest request
    ) {
        return productService.createProduct(request);
    }

    @GetMapping
    public List<ProductResponse> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    public ProductResponse getProduct(
            @PathVariable UUID id
    ) {
        return productService.getProduct(id);
    }

    @PutMapping("/{id}")
    public ProductResponse updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        return productService.updateProduct(id, request);
    }

    @PatchMapping("/{id}")
    public ProductResponse patchProduct(
            @PathVariable UUID id,
            @Valid @RequestBody PatchProductRequest request
    ) {
        return productService.patchProduct(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteProduct(
            @PathVariable UUID id
    ) {
        productService.deleteProduct(id);
    }
}