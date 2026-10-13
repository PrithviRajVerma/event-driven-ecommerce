package com.eventdriven.payment.e2e;

import com.eventdriven.events.KafkaTopics;
import com.eventdriven.events.inventory.InventoryReservedEvent;
import com.eventdriven.events.payment.PaymentCompletedEvent;
import com.eventdriven.events.payment.PaymentFailedEvent;
import com.eventdriven.payment.consumer.InventoryReservedConsumer;
import com.eventdriven.payment.domain.Payment;
import com.eventdriven.payment.domain.PaymentStatus;
import com.eventdriven.payment.idempotency.IdempotencyService;
import com.eventdriven.payment.outbox.OutboxEvent;
import com.eventdriven.payment.outbox.OutboxEventRepository;
import com.eventdriven.payment.outbox.OutboxPublisher;
import com.eventdriven.payment.outbox.OutboxService;
import com.eventdriven.payment.outbox.OutboxStatus;
import com.eventdriven.payment.producer.PaymentEventProducer;
import com.eventdriven.payment.repository.PaymentRepository;
import com.eventdriven.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * End-to-End Payment Saga Test.
 *
 * Verifies:
 * 1. InventoryReservedEvent triggers payment execution & PaymentCompletedEvent outbox emission
 * 2. Over-limit transaction triggers PaymentFailedEvent & compensation emission
 * 3. Outbox event relaying to Kafka broker
 * 4. Idempotent processing of duplicate payment triggers
 */
@ExtendWith(MockitoExtension.class)
class EndToEndPaymentSagaTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @Mock
    private OutboxService outboxService;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private IdempotencyService idempotencyService;

    private PaymentService paymentService;
    private InventoryReservedConsumer inventoryReservedConsumer;
    private OutboxPublisher outboxPublisher;

    private UUID orderId;
    private UUID customerId;
    private UUID paymentId;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, paymentEventProducer, outboxService);
        inventoryReservedConsumer = new InventoryReservedConsumer(paymentService, idempotencyService);
        outboxPublisher = new OutboxPublisher(outboxEventRepository, kafkaTemplate);

        orderId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Saga Step: InventoryReserved ➔ Payment Authorized ➔ PaymentCompletedEvent Saved in Outbox & Relayed")
    void testInventoryReserved_PaymentAuthorized_OutboxRelayed() {
        UUID eventId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("149.99");

        InventoryReservedEvent inventoryReservedEvent = InventoryReservedEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .customerId(customerId)
                .totalAmount(amount)
                .reservedItems(List.of())
                .build();

        when(idempotencyService.isAlreadyProcessed(eventId)).thenReturn(false);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(paymentId);
            return p;
        });

        // Trigger consumer
        inventoryReservedConsumer.handleInventoryReserved(inventoryReservedEvent);

        // Verify Payment persisted with COMPLETED
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, atLeastOnce()).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(savedPayment.getTransactionReference()).startsWith("TXN-");

        // Verify PaymentCompletedEvent published & saved to Outbox
        verify(paymentEventProducer).publishPaymentCompleted(any(PaymentCompletedEvent.class));
        verify(outboxService).saveEvent(
                eq("PAYMENT"),
                eq(paymentId.toString()),
                eq(KafkaTopics.PAYMENT_COMPLETED),
                any(PaymentCompletedEvent.class)
        );

        // Verify idempotency recorded
        verify(idempotencyService).markAsProcessed(eq(eventId), eq(inventoryReservedEvent.getEventType()), eq("payment-service-group"));

        // Step 2: Outbox relay to Kafka
        OutboxEvent outboxEvent = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("PAYMENT")
                .aggregateId(paymentId.toString())
                .eventType(KafkaTopics.PAYMENT_COMPLETED)
                .payload("{\"paymentId\":\"" + paymentId + "\"}")
                .status(OutboxStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();

        when(outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(outboxEvent));
        when(kafkaTemplate.send(eq(KafkaTopics.PAYMENT_COMPLETED), eq(paymentId.toString()), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        outboxPublisher.publishPendingEvents();

        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(outboxEvent.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("Saga Failure Branch: Limit Exceeded ➔ Payment Fails ➔ PaymentFailedEvent Emitted for Compensation")
    void testInventoryReserved_LimitExceeded_EmitsPaymentFailed() {
        UUID eventId = UUID.randomUUID();
        BigDecimal overLimitAmount = new BigDecimal("15000.00"); // Max is $10,000

        InventoryReservedEvent inventoryReservedEvent = InventoryReservedEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .customerId(customerId)
                .totalAmount(overLimitAmount)
                .reservedItems(List.of())
                .build();

        when(idempotencyService.isAlreadyProcessed(eventId)).thenReturn(false);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(paymentId);
            return p;
        });

        inventoryReservedConsumer.handleInventoryReserved(inventoryReservedEvent);

        // Verify Payment marked FAILED
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, atLeastOnce()).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(savedPayment.getFailureReason()).contains("Transaction limit exceeded");

        // Verify PaymentFailedEvent published & saved to Outbox
        verify(paymentEventProducer).publishPaymentFailed(any(PaymentFailedEvent.class));
        verify(outboxService).saveEvent(
                eq("PAYMENT"),
                eq(paymentId.toString()),
                eq(KafkaTopics.PAYMENT_FAILED),
                any(PaymentFailedEvent.class)
        );

        verify(idempotencyService).markAsProcessed(eq(eventId), eq(inventoryReservedEvent.getEventType()), eq("payment-service-group"));
    }

    @Test
    @DisplayName("Idempotency: Duplicate InventoryReservedEvent is skipped without double-charging")
    void testDuplicateInventoryReserved_IsDeduplicated() {
        UUID dupEventId = UUID.randomUUID();
        InventoryReservedEvent inventoryReservedEvent = InventoryReservedEvent.builder()
                .eventId(dupEventId)
                .orderId(orderId)
                .customerId(customerId)
                .totalAmount(new BigDecimal("100.00"))
                .build();

        when(idempotencyService.isAlreadyProcessed(dupEventId)).thenReturn(true);

        inventoryReservedConsumer.handleInventoryReserved(inventoryReservedEvent);

        verifyNoInteractions(paymentRepository);
        verifyNoInteractions(paymentEventProducer);
        verifyNoInteractions(outboxService);
    }
}
