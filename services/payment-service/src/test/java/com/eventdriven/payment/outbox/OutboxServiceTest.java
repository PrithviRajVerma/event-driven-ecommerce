package com.eventdriven.payment.outbox;

import com.eventdriven.events.payment.PaymentCompletedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private OutboxService outboxService;

    @Test
    @DisplayName("saveEvent serializes payload to JSON and saves OutboxEvent with PENDING status")
    void saveEvent_Success() {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .customerId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .occurredAt(Instant.now())
                .build();

        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(i -> i.getArgument(0));

        OutboxEvent saved = outboxService.saveEvent("PAYMENT", paymentId.toString(), "payment.completed", event);

        assertThat(saved).isNotNull();
        assertThat(saved.getAggregateType()).isEqualTo("PAYMENT");
        assertThat(saved.getAggregateId()).isEqualTo(paymentId.toString());
        assertThat(saved.getEventType()).isEqualTo("payment.completed");
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(saved.getPayload()).contains(paymentId.toString());
        assertThat(saved.getPayloadClass()).isEqualTo(PaymentCompletedEvent.class.getName());

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OutboxStatus.PENDING);
    }
}
