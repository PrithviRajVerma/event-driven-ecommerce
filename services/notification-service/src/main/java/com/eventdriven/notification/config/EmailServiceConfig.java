package com.eventdriven.notification.config;

import com.eventdriven.notification.service.EmailService;
import com.eventdriven.notification.service.MockEmailService;
import com.eventdriven.notification.service.ResendEmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dynamic configuration for EmailService:
 * - Uses ResendEmailService when 'resend.api-key' is provided.
 * - Gracefully falls back to MockEmailService when no API key is specified (dev / tests).
 */
@Slf4j
@Configuration
public class EmailServiceConfig {

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${resend.api-key:}')")
    public EmailService resendEmailService(ResendProperties properties) {
        log.info("Configuring live ResendEmailService with from: {}", properties.getFromEmail());
        return new ResendEmailService(properties);
    }

    @Bean
    @ConditionalOnMissingBean(EmailService.class)
    public EmailService mockEmailService() {
        log.info("No 'resend.api-key' detected. Initializing MockEmailService for local development and testing.");
        return new MockEmailService();
    }
}
