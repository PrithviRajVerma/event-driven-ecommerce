package com.eventdriven.auth.exception;

import org.springframework.http.HttpStatus;

public class InvalidRefreshTokenException extends AuthServiceException{
    public InvalidRefreshTokenException() {
        super(
                HttpStatus.UNAUTHORIZED,
                "INVALID_REFRESH_TOKEN",
                "Refresh token is invalid."
        );
    }
}
