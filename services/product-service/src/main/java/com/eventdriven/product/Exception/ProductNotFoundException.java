package com.eventdriven.product.Exception;

import org.springframework.http.HttpStatus;

public class ProductNotFoundException extends ProductServiceException {
    public ProductNotFoundException() {
        super(
                HttpStatus.NOT_FOUND,
                "PRODUCT_NOT_FOUND",
                "Product not found");
    }
}
