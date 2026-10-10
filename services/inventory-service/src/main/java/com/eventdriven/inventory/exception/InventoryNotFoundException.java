package com.eventdriven.inventory.exception;

import org.springframework.http.HttpStatus;

public class InventoryNotFoundException extends InventoryServiceException {

    public InventoryNotFoundException() {
        this("Inventory not found");
    }

    public InventoryNotFoundException(String message) {
        super(
                HttpStatus.NOT_FOUND,
                "INVENTORY_NOT_FOUND",
                message
        );
    }
}