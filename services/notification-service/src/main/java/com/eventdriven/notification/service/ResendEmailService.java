package com.eventdriven.notification.service;

import com.eventdriven.notification.config.ResendProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Production Email Service integrating with Resend (https://resend.com) developer email API.
 */
@Slf4j
public class ResendEmailService implements EmailService {

    private final ResendProperties properties;
    private final RestClient restClient;

    public ResendEmailService(ResendProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getApiUrl() != null && !properties.getApiUrl().isBlank()
                        ? properties.getApiUrl()
                        : "https://api.resend.com/emails")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // Constructor for testing with mocked/custom RestClient
    public ResendEmailService(ResendProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        String recipient = (properties.getDefaultRecipient() != null && !properties.getDefaultRecipient().isBlank())
                ? properties.getDefaultRecipient()
                : to;

        String from = (properties.getFromEmail() != null && !properties.getFromEmail().isBlank())
                ? properties.getFromEmail()
                : "onboarding@resend.dev";

        log.info("Dispatching live email via Resend API to: {} (original target: {}) with subject: '{}'",
                recipient, to, subject);

        Map<String, Object> payload = Map.of(
                "from", from,
                "to", List.of(recipient),
                "subject", subject,
                "text", body
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .body(payload)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("id")) {
                log.info("Email successfully sent via Resend API. Resend Email ID: {}", response.get("id"));
            } else {
                log.warn("Resend API response did not contain an email ID: {}", response);
            }
        } catch (RestClientResponseException ex) {
            log.error("Resend API returned an error response. Status: {}, Body: {}",
                    ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            throw new RuntimeException("Resend API error: " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            log.error("Unexpected error during Resend email dispatch: {}", ex.getMessage(), ex);
            throw new RuntimeException("Resend dispatch failure: " + ex.getMessage(), ex);
        }
    }
}
