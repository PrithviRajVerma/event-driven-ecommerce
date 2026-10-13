package com.eventdriven.gateway.service;

import com.eventdriven.gateway.config.GatewayProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayProxyService {

    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "host",
            "connection",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade",
            "content-length"
    );

    private final GatewayProperties gatewayProperties;
    private final RestClient restClient = RestClient.builder().build();

    public ResponseEntity<byte[]> proxyRequest(HttpServletRequest request, byte[] body, String correlationId) {
        String uriPath = request.getRequestURI();
        GatewayProperties.RouteDefinition targetRoute = findMatchingRoute(uriPath);

        if (targetRoute == null) {
            log.warn("No route matched for incoming path: {}", uriPath);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(("{\"error\":\"No gateway route found for path " + uriPath + "\"}").getBytes(StandardCharsets.UTF_8));
        }

        String targetUrl = targetRoute.getTargetUri() + uriPath;
        if (request.getQueryString() != null && !request.getQueryString().isBlank()) {
            targetUrl += "?" + request.getQueryString();
        }

        log.debug("Proxying request [{} {}] -> [{}] with correlation ID [{}]",
                request.getMethod(), uriPath, targetUrl, correlationId);

        try {
            HttpMethod method = HttpMethod.valueOf(request.getMethod());
            HttpHeaders headers = extractHeaders(request, correlationId);

            var requestSpec = restClient.method(method)
                    .uri(URI.create(targetUrl))
                    .headers(httpHeaders -> httpHeaders.putAll(headers));

            if (body != null && body.length > 0 && (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH)) {
                requestSpec.body(body);
            }

            return requestSpec.exchange((clientRequest, clientResponse) -> {
                byte[] responseBytes = clientResponse.getBody().readAllBytes();
                HttpHeaders responseHeaders = new HttpHeaders();
                clientResponse.getHeaders().forEach((name, values) -> {
                    if (!HOP_BY_HOP_HEADERS.contains(name.toLowerCase())) {
                        responseHeaders.put(name, values);
                    }
                });
                responseHeaders.set("X-Correlation-ID", correlationId);

                return ResponseEntity.status(clientResponse.getStatusCode())
                        .headers(responseHeaders)
                        .body(responseBytes);
            });
        } catch (RestClientResponseException ex) {
            log.warn("Downstream service responded with error [{}]: {}", ex.getStatusCode(), ex.getMessage());
            HttpHeaders responseHeaders = new HttpHeaders();
            ex.getResponseHeaders().forEach((name, values) -> {
                if (!HOP_BY_HOP_HEADERS.contains(name.toLowerCase())) {
                    responseHeaders.put(name, values);
                }
            });
            responseHeaders.set("X-Correlation-ID", correlationId);

            return ResponseEntity.status(ex.getStatusCode())
                    .headers(responseHeaders)
                    .body(ex.getResponseBodyAsByteArray());
        } catch (Exception ex) {
            log.error("Failed to proxy request to {}: {}", targetUrl, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Correlation-ID", correlationId)
                    .body(("{\"error\":\"Downstream service unavailable: " + targetRoute.getTargetUri() + "\",\"message\":\"" + ex.getMessage() + "\"}").getBytes(StandardCharsets.UTF_8));
        }
    }

    public GatewayProperties.RouteDefinition findMatchingRoute(String path) {
        if (path == null) {
            return null;
        }

        return gatewayProperties.getRoutes().values().stream()
                .filter(route -> route.getPathPrefix() != null && path.startsWith(route.getPathPrefix()))
                .findFirst()
                .orElse(null);
    }

    private HttpHeaders extractHeaders(HttpServletRequest request, String correlationId) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                if (!HOP_BY_HOP_HEADERS.contains(headerName.toLowerCase())) {
                    Enumeration<String> values = request.getHeaders(headerName);
                    while (values.hasMoreElements()) {
                        headers.add(headerName, values.nextElement());
                    }
                }
            }
        }
        headers.set("X-Correlation-ID", correlationId);
        return headers;
    }
}
