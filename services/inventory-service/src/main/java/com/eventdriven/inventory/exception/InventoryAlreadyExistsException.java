package com.eventdriven.inventory.exception;

import org.springframework.http.HttpStatus;

public class InventoryAlreadyExistsException extends InventoryServiceException{
    public InventoryAlreadyExistsException() {
        super(
                HttpStatus.CONFLICT,
                "INVENTORY_ALREADY_EXISTS_EXCEPTION",
                "Inventory already exists."
                );
    }
}
