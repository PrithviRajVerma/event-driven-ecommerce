package com.eventdriven.payment.dto;

import com.eventdriven.payment.domain.PaymentMethod;
import com.eventdriven.payment.domain.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private UUID id;
    private UUID orderId;
    private UUID customerId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private PaymentMethod paymentMethod;
    private String transactionReference;
    private String failureReason;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
