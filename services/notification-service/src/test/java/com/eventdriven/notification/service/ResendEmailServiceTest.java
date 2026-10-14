package com.eventdriven.notification.service;

import com.eventdriven.notification.config.ResendProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ResendEmailServiceTest {

    private ResendProperties properties;
    private MockRestServiceServer server;
    private ResendEmailService resendEmailService;

    @BeforeEach
    void setUp() {
        properties = new ResendProperties();
        properties.setApiKey("re_test_key_12345");
        properties.setFromEmail("onboarding@resend.dev");
        properties.setApiUrl("https://api.resend.com/emails");

        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getApiUrl());
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        resendEmailService = new ResendEmailService(properties, restClient);
    }

    @Test
    @DisplayName("sendEmail dispatches POST request with Authorization and JSON payload")
    void sendEmail_Success() {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer re_test_key_12345"))
                .andExpect(jsonPath("$.from").value("onboarding@resend.dev"))
                .andExpect(jsonPath("$.to[0]").value("buyer@example.com"))
                .andExpect(jsonPath("$.subject").value("Order Confirmed"))
                .andExpect(jsonPath("$.text").value("Thank you for your purchase!"))
                .andRespond(withSuccess("{\"id\": \"email_12345\"}", MediaType.APPLICATION_JSON));

        assertThatCode(() -> resendEmailService.sendEmail("buyer@example.com", "Order Confirmed", "Thank you for your purchase!"))
                .doesNotThrowAnyException();

        server.verify();
    }

    @Test
    @DisplayName("sendEmail routes to defaultRecipient when configured (developer sandbox mode)")
    void sendEmail_WithDefaultRecipientOverride() {
        properties.setDefaultRecipient("dev-account@example.com");

        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.to[0]").value("dev-account@example.com"))
                .andRespond(withSuccess("{\"id\": \"email_override_456\"}", MediaType.APPLICATION_JSON));

        resendEmailService.sendEmail("any-customer@random.com", "Receipt", "Body");

        server.verify();
    }

    @Test
    @DisplayName("sendEmail throws RuntimeException when Resend API returns error response")
    void sendEmail_ApiError_ThrowsException() {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withBadRequest().body("{\"message\": \"Invalid API key\", \"statusCode\": 400}"));

        assertThatThrownBy(() -> resendEmailService.sendEmail("buyer@example.com", "Subject", "Body"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Resend API error");

        server.verify();
    }
}
