package com.eventdriven.auth.exception;

import org.springframework.http.HttpStatus;

public class AccountDisabledException extends AuthServiceException{
    public AccountDisabledException() {
        super(
                HttpStatus.FORBIDDEN,
                "ACCOUNT_DISABLED",
                "Account is disabled."
        );
    }
}
