package com.eventdriven.inventory.exception;

import org.springframework.http.HttpStatus;

public class InsufficientStockException extends InventoryServiceException {

    public InsufficientStockException() {
        this("Insufficient stock available");
    }

    public InsufficientStockException(String message) {
        super(
                HttpStatus.CONFLICT,
                "INSUFFICIENT_STOCK",
                message
        );
    }
}