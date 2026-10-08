package com.eventdriven.order.exception;

import org.springframework.http.HttpStatus;

public class WishlistItemNotFoundException extends OrderServiceException {
    public WishlistItemNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "WISHLIST_ITEM_NOT_FOUND");
    }
}
