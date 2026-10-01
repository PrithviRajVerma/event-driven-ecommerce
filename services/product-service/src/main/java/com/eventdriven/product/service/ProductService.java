package com.eventdriven.product.service;

import com.eventdriven.product.Exception.ProductNotFoundException;
import com.eventdriven.product.Mapper.ProductMapper;
import com.eventdriven.product.dto.CreateProductRequest;
import com.eventdriven.product.dto.PatchProductRequest;
import com.eventdriven.product.dto.ProductResponse;
import com.eventdriven.product.dto.UpdateProductRequest;
import com.eventdriven.product.entity.Product;
import com.eventdriven.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductResponse createProduct(CreateProductRequest request) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        Product product = new Product();
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setCurrency(request.currency().toUpperCase(Locale.ROOT));
        product.setStockQuantity(request.stockQuantity());
        product.setImageUrl(request.imageUrl());
        product.setActive(true);
        product.setCreatedAt(now);
        product.setUpdatedAt(now);

        Product savedProduct = productRepository.save(product);

        return productMapper.toProductResponse(savedProduct);
    }

    @Transactional(readOnly = true)
    public ProductResponse getActiveProduct(UUID id) {
        Product product =  productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(ProductNotFoundException::new);

        return productMapper.toProductResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID id) {
        Product product =  productRepository.findById(id)
                .orElseThrow(ProductNotFoundException::new);

        return productMapper.toProductResponse(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                            .map(productMapper::toProductResponse)
                            .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllActiveProducts() {
        return productRepository.findByActiveTrue().stream()
                .map(productMapper::toProductResponse)
                .toList();
    }

    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                        .orElseThrow(ProductNotFoundException::new);

        product.setActive(false);
        product.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        productRepository.save(product);
    }

    public ProductResponse updateProduct(UUID id, UpdateProductRequest request) {

        Product product = productRepository.findById(id)
                .orElseThrow(ProductNotFoundException::new);

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setCurrency(request.currency().toUpperCase(Locale.ROOT));
        product.setImageUrl(request.imageUrl());
        product.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        return productMapper.toProductResponse(product);
    }

    public ProductResponse patchProduct(UUID id, PatchProductRequest request) {

        Product product = productRepository.findById(id)
                .orElseThrow(ProductNotFoundException::new);

        if (request.name() != null) {
            product.setName(request.name());
        }

        if (request.description() != null) {
            product.setDescription(request.description());
        }

        if (request.price() != null) {
            product.setPrice(request.price());
        }

        if (request.currency() != null) {
            product.setCurrency(
                    request.currency().toUpperCase(Locale.ROOT)
            );
        }

        if (request.imageUrl() != null) {
            product.setImageUrl(request.imageUrl());
        }

        product.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        return productMapper.toProductResponse(product);
    }
}