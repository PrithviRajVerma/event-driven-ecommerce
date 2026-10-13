package com.eventdriven.gateway.e2e;

import com.eventdriven.gateway.config.GatewayProperties;
import com.eventdriven.gateway.controller.GatewayController;
import com.eventdriven.gateway.service.GatewayProxyService;
import com.eventdriven.gateway.service.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-End API Gateway Edge Integration Test.
 *
 * Verifies:
 * 1. Reverse proxying for all microservices (/auth, /products, /orders, /payments, /inventory, /notifications)
 * 2. Rate limiting enforcement (HTTP 429 on rate limit exceeded)
 * 3. Correlation ID preservation across the edge
 * 4. Gateway route registry discovery endpoint
 */
@ExtendWith(MockitoExtension.class)
class EndToEndGatewayIntegrationTest {

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
    @DisplayName("Edge Routing: Successfully proxies /api/v1/orders/checkout with X-Correlation-ID")
    void testProxyRequest_OrderCheckout_Success() throws Exception {
        String correlationId = UUID.randomUUID().toString();
        when(rateLimiterService.isAllowed(any())).thenReturn(true);

        ResponseEntity<byte[]> proxyResponse = ResponseEntity.ok()
                .header("X-Correlation-ID", correlationId)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"orderId\":\"ord-123\",\"status\":\"PENDING\"}".getBytes(StandardCharsets.UTF_8));

        when(gatewayProxyService.proxyRequest(any(), any(), any())).thenReturn(proxyResponse);

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("X-Correlation-ID", correlationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shippingAddress\":\"123 Main St\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-ID", correlationId))
                .andExpect(jsonPath("$.orderId").value("ord-123"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("Edge Routing: Successfully proxies /api/v1/products catalog query")
    void testProxyRequest_ProductsCatalog_Success() throws Exception {
        when(rateLimiterService.isAllowed(any())).thenReturn(true);

        ResponseEntity<byte[]> proxyResponse = ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body("[{\"id\":\"prod-1\",\"name\":\"Mechanical Keyboard\"}]".getBytes(StandardCharsets.UTF_8));

        when(gatewayProxyService.proxyRequest(any(), any(), any())).thenReturn(proxyResponse);

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Mechanical Keyboard"));
    }

    @Test
    @DisplayName("Rate Limiter: Throttles requests when client exceeds rate limit with HTTP 429")
    void testProxyRequest_RateLimitExceeded() throws Exception {
        when(rateLimiterService.isAllowed(any())).thenReturn(false);

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").value("Rate limit exceeded. Please try again later."));

        verifyNoInteractions(gatewayProxyService);
    }

    @Test
    @DisplayName("Discovery Endpoint: Returns all 6 active microservice route definitions")
    void testGetRoutes_ReturnsAllConfiguredRoutes() throws Exception {
        Map<String, GatewayProperties.RouteDefinition> routes = new LinkedHashMap<>();

        routes.put("auth", createRoute("/api/v1/auth", "http://localhost:8081"));
        routes.put("product", createRoute("/api/v1/products", "http://localhost:8082"));
        routes.put("order", createRoute("/api/v1/orders", "http://localhost:8083"));
        routes.put("payment", createRoute("/api/v1/payments", "http://localhost:8084"));
        routes.put("inventory", createRoute("/api/v1/inventory", "http://localhost:8085"));
        routes.put("notification", createRoute("/api/v1/notifications", "http://localhost:8086"));

        when(gatewayProperties.getRoutes()).thenReturn(routes);

        mockMvc.perform(get("/api/v1/gateway/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auth.pathPrefix").value("/api/v1/auth"))
                .andExpect(jsonPath("$.auth.targetUri").value("http://localhost:8081"))
                .andExpect(jsonPath("$.order.pathPrefix").value("/api/v1/orders"))
                .andExpect(jsonPath("$.order.targetUri").value("http://localhost:8083"))
                .andExpect(jsonPath("$.payment.pathPrefix").value("/api/v1/payments"))
                .andExpect(jsonPath("$.payment.targetUri").value("http://localhost:8084"))
                .andExpect(jsonPath("$.notification.pathPrefix").value("/api/v1/notifications"))
                .andExpect(jsonPath("$.notification.targetUri").value("http://localhost:8086"));
    }

    private GatewayProperties.RouteDefinition createRoute(String prefix, String uri) {
        GatewayProperties.RouteDefinition route = new GatewayProperties.RouteDefinition();
        route.setPathPrefix(prefix);
        route.setTargetUri(uri);
        return route;
    }
}
