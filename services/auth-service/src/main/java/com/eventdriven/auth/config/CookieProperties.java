package com.eventdriven.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.cookies")
public record CookieProperties (
    boolean secure,
    String sameSite
){
}
