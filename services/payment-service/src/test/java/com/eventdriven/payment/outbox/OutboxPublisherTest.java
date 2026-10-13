package com.eventdriven.payment.outbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OutboxPublisher outboxPublisher;

    @Test
    @DisplayName("publishPendingEvents publishes to Kafka and updates status to PUBLISHED")
    void publishPendingEvents_Success() {
        UUID eventId = UUID.randomUUID();
        OutboxEvent event = OutboxEvent.builder()
                .id(eventId)
                .aggregateType("PAYMENT")
                .aggregateId("payment-123")
                .eventType("payment.completed")
                .payload("{\"paymentId\":\"payment-123\"}")
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        when(outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(eq("payment.completed"), eq("payment-123"), any())).thenReturn(future);

        outboxPublisher.publishPendingEvents();

        verify(kafkaTemplate).send(eq("payment.completed"), eq("payment-123"), any());
        verify(outboxEventRepository).save(event);
    }

    @Test
    @DisplayName("publishPendingEvents does nothing when no pending events exist")
    void publishPendingEvents_EmptyList() {
        when(outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of());

        outboxPublisher.publishPendingEvents();

        verifyNoInteractions(kafkaTemplate);
    }
}
