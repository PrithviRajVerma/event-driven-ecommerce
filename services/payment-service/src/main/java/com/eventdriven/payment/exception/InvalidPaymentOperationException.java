package com.eventdriven.payment.exception;

import org.springframework.http.HttpStatus;

public class InvalidPaymentOperationException extends PaymentServiceException {

    public InvalidPaymentOperationException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_PAYMENT_OPERATION");
    }
}
