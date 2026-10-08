package com.eventdriven.order.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class OrderServiceException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String errorCode;

    public OrderServiceException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
}
