package com.eventdriven.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cart.guest")
public record CartProperties(
        int ttlDays
) {}
