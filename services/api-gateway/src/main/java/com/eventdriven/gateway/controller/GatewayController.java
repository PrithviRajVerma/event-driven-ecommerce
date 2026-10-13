package com.eventdriven.gateway.controller;

import com.eventdriven.gateway.config.GatewayProperties;
import com.eventdriven.gateway.filter.CorrelationIdFilter;
import com.eventdriven.gateway.service.GatewayProxyService;
import com.eventdriven.gateway.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class GatewayController {

    private final GatewayProxyService gatewayProxyService;
    private final RateLimiterService rateLimiterService;
    private final GatewayProperties gatewayProperties;

    @GetMapping("/api/v1/gateway/routes")
    public ResponseEntity<Map<String, GatewayProperties.RouteDefinition>> getRoutes() {
        return ResponseEntity.ok(gatewayProperties.getRoutes());
    }

    @RequestMapping(value = "/api/v1/**", method = {
            RequestMethod.GET,
            RequestMethod.POST,
            RequestMethod.PUT,
            RequestMethod.PATCH,
            RequestMethod.DELETE
    })
    public ResponseEntity<byte[]> proxyRequest(
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body,
            @RequestAttribute(value = CorrelationIdFilter.CORRELATION_ID_HEADER, required = false) String correlationId
    ) {
        String clientIp = resolveClientIp(request);

        if (!rateLimiterService.isAllowed(clientIp)) {
            log.warn("Rate limit exceeded for client IP: {}", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Correlation-ID", correlationId)
                    .body("{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}".getBytes(StandardCharsets.UTF_8));
        }

        return gatewayProxyService.proxyRequest(request, body, correlationId);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
