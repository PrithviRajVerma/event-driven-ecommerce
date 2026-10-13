package com.eventdriven.gateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService();
        ReflectionTestUtils.setField(rateLimiterService, "enabled", true);
        ReflectionTestUtils.setField(rateLimiterService, "limitPerMinute", 5);
    }

    @Test
    @DisplayName("Allows requests up to limitPerMinute")
    void isAllowed_UnderLimit_ReturnsTrue() {
        String clientId = "client-1";
        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiterService.isAllowed(clientId)).isTrue();
        }
    }

    @Test
    @DisplayName("Blocks requests exceeding limitPerMinute")
    void isAllowed_OverLimit_ReturnsFalse() {
        String clientId = "client-2";
        for (int i = 0; i < 5; i++) {
            rateLimiterService.isAllowed(clientId);
        }
        assertThat(rateLimiterService.isAllowed(clientId)).isFalse();
    }

    @Test
    @DisplayName("Always allows when disabled")
    void isAllowed_Disabled_ReturnsTrue() {
        ReflectionTestUtils.setField(rateLimiterService, "enabled", false);
        for (int i = 0; i < 100; i++) {
            assertThat(rateLimiterService.isAllowed("client-3")).isTrue();
        }
    }
}
