package com.eventdriven.payment.exception;

import org.springframework.http.HttpStatus;

public class PaymentProcessingException extends PaymentServiceException {

    public PaymentProcessingException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_CONTENT, "PAYMENT_PROCESSING_FAILED");
    }
}
