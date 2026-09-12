package com.eventdriven.auth.exception;

import org.springframework.http.HttpStatus;
public class InvalidEmailVerificationTokenException extends AuthServiceException{

    public InvalidEmailVerificationTokenException(){
        super(
                HttpStatus.BAD_REQUEST,
                "INVALID_VERIFICATIOIN_TOKEN",
                "Verification token is invalid"
        );
    }
}
