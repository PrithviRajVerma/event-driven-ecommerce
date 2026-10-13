package com.eventdriven.gateway.controller;

import com.eventdriven.gateway.config.GatewayProperties;
import com.eventdriven.gateway.service.GatewayProxyService;
import com.eventdriven.gateway.service.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GatewayControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GatewayProxyService gatewayProxyService;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private GatewayProperties gatewayProperties;

    @InjectMocks
    private GatewayController gatewayController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(gatewayController).build();
    }

    @Test
    @DisplayName("GET /api/v1/gateway/routes returns configured route definitions")
    void getRoutes_Success() throws Exception {
        Map<String, GatewayProperties.RouteDefinition> routes = new LinkedHashMap<>();
        GatewayProperties.RouteDefinition route = new GatewayProperties.RouteDefinition();
        route.setPathPrefix("/api/v1/auth");
        route.setTargetUri("http://localhost:8081");
        routes.put("auth", route);

        when(gatewayProperties.getRoutes()).thenReturn(routes);

        mockMvc.perform(get("/api/v1/gateway/routes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auth.pathPrefix").value("/api/v1/auth"))
                .andExpect(jsonPath("$.auth.targetUri").value("http://localhost:8081"));
    }

    @Test
    @DisplayName("Returns 429 TOO_MANY_REQUESTS when rate limit is exceeded")
    void proxyRequest_RateLimitExceeded_Returns429() throws Exception {
        when(rateLimiterService.isAllowed(any())).thenReturn(false);

        mockMvc.perform(get("/api/v1/products/123"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Too Many Requests"));
    }

    @Test
    @DisplayName("Proxies request when rate limiter allows")
    void proxyRequest_Allowed_ProxiesSuccessfully() throws Exception {
        when(rateLimiterService.isAllowed(any())).thenReturn(true);
        ResponseEntity<byte[]> proxyResponse = ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"id\":\"prod-123\"}".getBytes(StandardCharsets.UTF_8));

        when(gatewayProxyService.proxyRequest(any(), any(), any())).thenReturn(proxyResponse);

        mockMvc.perform(get("/api/v1/products/123"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"id\":\"prod-123\"}"));
    }
}
