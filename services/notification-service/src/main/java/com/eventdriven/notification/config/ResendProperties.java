package com.eventdriven.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "resend")
public class ResendProperties {

    /**
     * Resend API Key (starts with re_...).
     * If blank or not provided, Notification Service falls back to MockEmailService.
     */
    private String apiKey;

    /**
     * Sender email address.
     * Default for Resend sandbox is "onboarding@resend.dev".
     * In production with verified domain, e.g. "orders@yourdomain.com".
     */
    private String fromEmail = "onboarding@resend.dev";

    /**
     * Optional default recipient override.
     * In Resend free sandbox tier, emails can only be sent to the registered account email.
     * If set, all outbound emails are delivered to this address.
     */
    private String defaultRecipient;

    /**
     * Resend API endpoint.
     */
    private String apiUrl = "https://api.resend.com/emails";
}
