package com.eventdriven.payment.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class PaymentServiceException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String errorCode;

    protected PaymentServiceException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
}
