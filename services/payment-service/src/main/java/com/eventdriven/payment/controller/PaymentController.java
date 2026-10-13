package com.eventdriven.payment.controller;

import com.eventdriven.payment.dto.PaymentResponse;
import com.eventdriven.payment.dto.ProcessPaymentRequest;
import com.eventdriven.payment.exception.UnauthorizedPaymentAccessException;
import com.eventdriven.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment authorization, transaction lookup, and customer payment history APIs")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/process")
    @Operation(
            summary = "Process or simulate a payment",
            description = "Submits a payment request for an order. Emits PaymentCompletedEvent on success or PaymentFailedEvent on failure to Kafka."
    )
    public ResponseEntity<PaymentResponse> processPayment(
            Authentication authentication,
            @Valid @RequestBody ProcessPaymentRequest request
    ) {
        UUID customerId = extractCustomerId(authentication);
        if (request.getCustomerId() == null && customerId != null) {
            request.setCustomerId(customerId);
        }
        PaymentResponse response = paymentService.processPayment(request);
        HttpStatus status = response.getStatus().name().equals("COMPLETED") ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/order/{orderId}")
    @Operation(
            summary = "Get payment by order ID",
            description = "Retrieves payment details for a specific order owned by the authenticated customer."
    )
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(
            Authentication authentication,
            @Parameter(description = "Order ID to look up")
            @PathVariable UUID orderId
    ) {
        UUID customerId = extractCustomerId(authentication);
        if (customerId == null) {
            throw new UnauthorizedPaymentAccessException("Authentication required to view payment");
        }
        PaymentResponse response = paymentService.getPaymentByOrderId(orderId, customerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get payment by ID",
            description = "Retrieves payment transaction details by payment ID."
    )
    public ResponseEntity<PaymentResponse> getPaymentById(
            Authentication authentication,
            @Parameter(description = "Payment ID to look up")
            @PathVariable UUID id
    ) {
        UUID customerId = extractCustomerId(authentication);
        if (customerId == null) {
            throw new UnauthorizedPaymentAccessException("Authentication required to view payment");
        }
        PaymentResponse response = paymentService.getPaymentById(id, customerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
            summary = "List customer payments",
            description = "Retrieves complete transaction payment history for the authenticated customer."
    )
    public ResponseEntity<List<PaymentResponse>> getCustomerPayments(Authentication authentication) {
        UUID customerId = extractCustomerId(authentication);
        if (customerId == null) {
            throw new UnauthorizedPaymentAccessException("Authentication required to view payment history");
        }
        List<PaymentResponse> responses = paymentService.getCustomerPayments(customerId);
        return ResponseEntity.ok(responses);
    }

    private UUID extractCustomerId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UUID userId) {
            return userId;
        }
        return null;
    }
}
