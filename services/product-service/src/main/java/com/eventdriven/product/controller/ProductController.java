package com.eventdriven.product.controller;

import com.eventdriven.product.dto.ProductResponse;
import com.eventdriven.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<ProductResponse> getAllActiveProducts() {
        return productService.getAllActiveProducts();
    }
    @GetMapping("/{id}")
    public ProductResponse getActiveProduct(
            @PathVariable UUID id
    ) {
        return productService.getActiveProduct(id);
    }

}