package com.eventdriven.order.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class OrderNotFoundException extends OrderServiceException {

    public OrderNotFoundException(UUID orderId) {
        super("Order not found with ID: " + orderId, HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND");
    }

    public OrderNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND");
    }
}
