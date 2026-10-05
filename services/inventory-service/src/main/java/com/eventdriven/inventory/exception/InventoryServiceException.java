package com.eventdriven.inventory.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class InventoryServiceException extends RuntimeException{

    private final HttpStatus httpStatus;
    private final String errorCode;

    protected InventoryServiceException(
            HttpStatus httpStatus,
            String errorCode,
            String message
    ){
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

}
