package com.eventdriven.inventory.exception;

import org.springframework.http.HttpStatus;

public class InventoryNotFoundException extends InventoryServiceException {

    public InventoryNotFoundException() {
        super(
                HttpStatus.NOT_FOUND,
                "INVENTORY_NOT_FOUND",
                "Inventory not found"
        );
    }
}