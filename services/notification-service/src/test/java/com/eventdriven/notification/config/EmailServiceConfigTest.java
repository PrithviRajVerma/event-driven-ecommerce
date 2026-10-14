package com.eventdriven.notification.config;

import com.eventdriven.notification.service.EmailService;
import com.eventdriven.notification.service.MockEmailService;
import com.eventdriven.notification.service.ResendEmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class EmailServiceConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EmailServiceConfig.class, ResendProperties.class));

    @Test
    @DisplayName("Defaults to MockEmailService when no resend.api-key is configured")
    void defaultsToMockEmailService_WhenNoApiKey() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(EmailService.class);
            assertThat(context.getBean(EmailService.class)).isInstanceOf(MockEmailService.class);
        });
    }

    @Test
    @DisplayName("Defaults to MockEmailService when resend.api-key is empty string")
    void defaultsToMockEmailService_WhenEmptyApiKey() {
        contextRunner.withPropertyValues("resend.api-key=")
                .run(context -> {
                    assertThat(context).hasSingleBean(EmailService.class);
                    assertThat(context.getBean(EmailService.class)).isInstanceOf(MockEmailService.class);
                });
    }

    @Test
    @DisplayName("Loads ResendEmailService when resend.api-key is provided")
    void loadsResendEmailService_WhenApiKeyPresent() {
        contextRunner.withPropertyValues("resend.api-key=re_123456789")
                .run(context -> {
                    assertThat(context).hasSingleBean(EmailService.class);
                    assertThat(context.getBean(EmailService.class)).isInstanceOf(ResendEmailService.class);
                });
    }
}
