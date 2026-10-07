package com.eventdriven.inventory.exception;

import org.springframework.http.HttpStatus;

public class InsufficientReservedStockException extends InventoryServiceException {

    public InsufficientReservedStockException() {
        super(
                HttpStatus.CONFLICT,
                "INSUFFICIENT_RESERVED_STOCK",
                "Reserved stock is insufficient for the requested operation"
        );
    }
}
