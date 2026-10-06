package com.eventdriven.inventory.exception;

import org.springframework.http.HttpStatus;

public class InsufficientStockException extends InventoryServiceException {

    public InsufficientStockException() {
        super(
                HttpStatus.CONFLICT,
                "INSUFFICIENT_STOCK",
                "Insufficient stock available"
        );
    }
}