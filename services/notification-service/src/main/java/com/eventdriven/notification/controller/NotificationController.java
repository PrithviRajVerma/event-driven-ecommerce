package com.eventdriven.notification.controller;

import com.eventdriven.notification.dto.NotificationResponse;
import com.eventdriven.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Endpoints for inspecting customer notification and communication audit logs")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get all notifications sent to a customer", description = "Retrieves communication history for a given customer ID ordered by creation date descending")
    public ResponseEntity<List<NotificationResponse>> getCustomerNotifications(@PathVariable UUID customerId) {
        List<NotificationResponse> responses = notificationService.getCustomerNotifications(customerId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get all notifications associated with an order", description = "Retrieves communication history for a given order ID ordered by creation date descending")
    public ResponseEntity<List<NotificationResponse>> getOrderNotifications(@PathVariable UUID orderId) {
        List<NotificationResponse> responses = notificationService.getOrderNotifications(orderId);
        return ResponseEntity.ok(responses);
    }
}
