package com.eventdriven.payment.dto;

import com.eventdriven.payment.domain.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPaymentRequest {

    @NotNull(message = "Order ID is required")
    private UUID orderId;

    private UUID customerId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @Builder.Default
    private String currency = "USD";

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
}
