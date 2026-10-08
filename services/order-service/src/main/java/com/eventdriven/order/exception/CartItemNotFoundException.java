package com.eventdriven.order.exception;

import org.springframework.http.HttpStatus;

public class CartItemNotFoundException extends OrderServiceException {
    public CartItemNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "CART_ITEM_NOT_FOUND");
    }
}
