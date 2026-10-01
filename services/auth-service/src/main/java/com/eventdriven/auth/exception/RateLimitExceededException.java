package com.eventdriven.auth.exception;


import org.springframework.http.HttpStatus;

public class RateLimitExceededException extends AuthServiceException{

    public RateLimitExceededException() {
        super(
                HttpStatus.TOO_MANY_REQUESTS,
                "TOO_MANY_REQUESTS",
                "Too many requests.Please try again later."
        );
    }
}
