package com.eventdriven.payment.producer;

import com.eventdriven.events.payment.PaymentCompletedEvent;
import com.eventdriven.events.payment.PaymentFailedEvent;
import com.eventdriven.payment.config.KafkaTopicConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private PaymentEventProducer paymentEventProducer;

    @Test
    @DisplayName("Publishes PaymentCompletedEvent with generated eventId and occurredAt")
    void publishPaymentCompleted_SetsDefaults() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .orderId(orderId)
                .customerId(customerId)
                .paymentId(paymentId)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .build();

        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq(KafkaTopicConfig.PAYMENT_COMPLETED_TOPIC), eq(orderId.toString()), eq(event)))
                .thenReturn(future);

        CompletableFuture<SendResult<String, Object>> result = paymentEventProducer.publishPaymentCompleted(event);

        assertThat(result).isNotNull();
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getOccurredAt()).isNotNull();
        verify(kafkaTemplate).send(KafkaTopicConfig.PAYMENT_COMPLETED_TOPIC, orderId.toString(), event);
    }

    @Test
    @DisplayName("Publishes PaymentFailedEvent with generated eventId and occurredAt")
    void publishPaymentFailed_SetsDefaults() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .orderId(orderId)
                .customerId(customerId)
                .amount(new BigDecimal("500.00"))
                .reason("Card declined")
                .build();

        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq(KafkaTopicConfig.PAYMENT_FAILED_TOPIC), eq(orderId.toString()), eq(event)))
                .thenReturn(future);

        CompletableFuture<SendResult<String, Object>> result = paymentEventProducer.publishPaymentFailed(event);

        assertThat(result).isNotNull();
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getOccurredAt()).isNotNull();
        verify(kafkaTemplate).send(KafkaTopicConfig.PAYMENT_FAILED_TOPIC, orderId.toString(), event);
    }
}
