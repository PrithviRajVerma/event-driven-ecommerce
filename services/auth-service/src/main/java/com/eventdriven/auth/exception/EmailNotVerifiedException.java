package com.eventdriven.auth.exception;

import org.springframework.http.HttpStatus;

public class EmailNotVerifiedException extends AuthServiceException{
    public EmailNotVerifiedException() {
        super(
                HttpStatus.FORBIDDEN,
                "EMAIL_NOT_VERIFIED",
                "Email address is not verified"
        );
    }
}
