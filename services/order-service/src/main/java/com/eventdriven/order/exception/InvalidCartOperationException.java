package com.eventdriven.order.exception;

import org.springframework.http.HttpStatus;

public class InvalidCartOperationException extends OrderServiceException {
    public InvalidCartOperationException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_CART_OPERATION");
    }
}
