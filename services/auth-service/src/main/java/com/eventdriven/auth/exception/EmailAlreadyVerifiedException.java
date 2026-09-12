package com.eventdriven.auth.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyVerifiedException extends AuthServiceException{

    public EmailAlreadyVerifiedException(){
        super(
                HttpStatus.CONFLICT,
                "EMAIL_ALREADY_VERIFIED",
                "Email is already verified."
        );
    }
}
