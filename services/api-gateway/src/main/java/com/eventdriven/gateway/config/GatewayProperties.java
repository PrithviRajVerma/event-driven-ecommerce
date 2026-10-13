package com.eventdriven.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    private Map<String, RouteDefinition> routes = new LinkedHashMap<>();

    @Data
    public static class RouteDefinition {
        private String pathPrefix;
        private String targetUri;
    }
}
