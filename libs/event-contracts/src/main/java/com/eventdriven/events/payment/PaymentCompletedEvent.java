package com.eventdriven.events.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {

    public static final String EVENT_TYPE = "PAYMENT_COMPLETED";

    private UUID eventId;

    @Builder.Default
    private String eventType = EVENT_TYPE;

    private Instant occurredAt;
    private UUID orderId;
    private UUID customerId;
    private UUID paymentId;
    private BigDecimal amount;
    private String currency;
}
