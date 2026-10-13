package com.eventdriven.payment.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedPaymentAccessException extends PaymentServiceException {

    public UnauthorizedPaymentAccessException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED_PAYMENT_ACCESS");
    }
}
