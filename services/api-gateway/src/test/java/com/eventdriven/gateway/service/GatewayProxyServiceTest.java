package com.eventdriven.gateway.service;

import com.eventdriven.gateway.config.GatewayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayProxyServiceTest {

    private GatewayProperties gatewayProperties;
    private GatewayProxyService gatewayProxyService;

    @BeforeEach
    void setUp() {
        gatewayProperties = new GatewayProperties();
        Map<String, GatewayProperties.RouteDefinition> routes = new LinkedHashMap<>();

        GatewayProperties.RouteDefinition authRoute = new GatewayProperties.RouteDefinition();
        authRoute.setPathPrefix("/api/v1/auth");
        authRoute.setTargetUri("http://localhost:8081");
        routes.put("auth", authRoute);

        GatewayProperties.RouteDefinition productRoute = new GatewayProperties.RouteDefinition();
        productRoute.setPathPrefix("/api/v1/products");
        productRoute.setTargetUri("http://localhost:8082");
        routes.put("product", productRoute);

        GatewayProperties.RouteDefinition orderRoute = new GatewayProperties.RouteDefinition();
        orderRoute.setPathPrefix("/api/v1/orders");
        orderRoute.setTargetUri("http://localhost:8083");
        routes.put("order", orderRoute);

        gatewayProperties.setRoutes(routes);
        gatewayProxyService = new GatewayProxyService(gatewayProperties);
    }

    @Test
    @DisplayName("Correctly matches route by path prefix")
    void findMatchingRoute_Success() {
        GatewayProperties.RouteDefinition route = gatewayProxyService.findMatchingRoute("/api/v1/auth/login");
        assertThat(route).isNotNull();
        assertThat(route.getTargetUri()).isEqualTo("http://localhost:8081");

        GatewayProperties.RouteDefinition prodRoute = gatewayProxyService.findMatchingRoute("/api/v1/products/123");
        assertThat(prodRoute).isNotNull();
        assertThat(prodRoute.getTargetUri()).isEqualTo("http://localhost:8082");
    }

    @Test
    @DisplayName("Returns null when path does not match any prefix")
    void findMatchingRoute_NoMatch_ReturnsNull() {
        GatewayProperties.RouteDefinition route = gatewayProxyService.findMatchingRoute("/api/v1/unknown");
        assertThat(route).isNull();
    }

    @Test
    @DisplayName("Returns 404 NOT_FOUND when proxying unmapped path")
    void proxyRequest_UnmappedPath_Returns404() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/unknown");
        ResponseEntity<byte[]> response = gatewayProxyService.proxyRequest(request, null, "test-corr-id");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(new String(response.getBody())).contains("No gateway route found");
    }
}
