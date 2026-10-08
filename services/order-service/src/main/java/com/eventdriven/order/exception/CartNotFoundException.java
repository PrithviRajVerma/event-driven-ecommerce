package com.eventdriven.order.exception;

import org.springframework.http.HttpStatus;

public class CartNotFoundException extends OrderServiceException {
    public CartNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "CART_NOT_FOUND");
    }
}
