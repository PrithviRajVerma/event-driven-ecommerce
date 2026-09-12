package com.eventdriven.auth.exception;


import org.springframework.http.HttpStatus;

public class EmailAlreadyRegisteredException extends AuthServiceException{

    public EmailAlreadyRegisteredException(){
        super(
                HttpStatus.CONFLICT,
                "EMAIL_ALREADY_REGISTERED",
                "email is already registered"
        );
    }

}
