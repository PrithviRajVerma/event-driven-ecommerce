package com.eventdriven.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MockEmailService implements EmailService {

    @Override
    public void sendEmail(String to, String subject, String body) {
        log.info("""
                ====================================================
                [MOCK EMAIL DISPATCH]
                To: {}
                Subject: {}
                Body:
                {}
                ====================================================
                """, to, subject, body);
    }
}
