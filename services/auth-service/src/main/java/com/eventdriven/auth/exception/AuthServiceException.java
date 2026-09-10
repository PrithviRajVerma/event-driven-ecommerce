package com.eventdriven.auth.exception;


import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class AuthServiceException extends RuntimeException{

    private final HttpStatus httpStatus;
    private final String errorCode;

    protected AuthServiceException(
            HttpStatus httpStatus,
            String errorCode,
            String message
    ){
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }



}
