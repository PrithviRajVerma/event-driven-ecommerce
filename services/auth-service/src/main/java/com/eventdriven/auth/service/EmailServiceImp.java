package com.eventdriven.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailServiceImp implements EmailService {

    @Override
    public void sendVerificationEmail(
            String email, String token
    ){

        String verificationLink =
                "http://localhost:8081/api/v1/auth/verify-email?token=" + token;

        log.info(
                "Verification email for {} -> {}",
                email,
                verificationLink
        );

    }
}
