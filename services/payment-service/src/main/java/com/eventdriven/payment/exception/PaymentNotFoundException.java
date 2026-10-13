package com.eventdriven.payment.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class PaymentNotFoundException extends PaymentServiceException {

    public PaymentNotFoundException(UUID paymentId) {
        super("Payment not found with id: " + paymentId, HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND");
    }

    public PaymentNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND");
    }
}
