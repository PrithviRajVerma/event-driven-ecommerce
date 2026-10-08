package com.eventdriven.order.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedCartAccessException extends OrderServiceException {
    public UnauthorizedCartAccessException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }
}
