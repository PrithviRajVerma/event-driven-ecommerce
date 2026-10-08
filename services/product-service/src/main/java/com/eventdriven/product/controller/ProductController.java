package com.eventdriven.product.controller;

import com.eventdriven.product.dto.ProductResponse;
import com.eventdriven.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Customer Products", description = "Public customer-facing product catalog endpoints")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Get all active products", description = "Retrieves the public catalog of active products.")
    public List<ProductResponse> getAllActiveProducts() {
        return productService.getAllActiveProducts();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get active product by ID", description = "Retrieves an active product by its unique identifier.")
    public ProductResponse getActiveProduct(@PathVariable UUID id) {
        return productService.getActiveProduct(id);
    }
}