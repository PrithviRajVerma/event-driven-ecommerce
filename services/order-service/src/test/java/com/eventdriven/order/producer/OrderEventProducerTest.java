package com.eventdriven.order.producer;

import com.eventdriven.events.order.OrderCancelledEvent;
import com.eventdriven.events.order.OrderConfirmedEvent;
import com.eventdriven.events.order.OrderCreatedEvent;
import com.eventdriven.order.config.KafkaTopicConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private OrderEventProducer orderEventProducer;

    @BeforeEach
    void setUp() {
        orderEventProducer = new OrderEventProducer(kafkaTemplate);
    }

    private CompletableFuture<SendResult<String, Object>> mockSuccessFuture(String topic, String key, Object payload) {
        ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(topic, key, payload);
        RecordMetadata metadata = new RecordMetadata(new TopicPartition(topic, 0), 0, 0, 0, 0, 0);
        SendResult<String, Object> sendResult = new SendResult<>(producerRecord, metadata);
        return CompletableFuture.completedFuture(sendResult);
    }

    @Test
    @DisplayName("publishOrderCreated sends event to order.created topic with orderId key")
    void publishOrderCreated_Success() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(orderId)
                .customerId(customerId)
                .totalAmount(new BigDecimal("99.99"))
                .items(List.of(
                        OrderCreatedEvent.OrderItem.builder()
                                .productId(productId)
                                .quantity(2)
                                .unitPrice(new BigDecimal("49.99"))
                                .build()
                ))
                .build();

        when(kafkaTemplate.send(eq(KafkaTopicConfig.ORDER_CREATED_TOPIC), eq(orderId.toString()), any()))
                .thenReturn(mockSuccessFuture(KafkaTopicConfig.ORDER_CREATED_TOPIC, orderId.toString(), event));

        CompletableFuture<SendResult<String, Object>> future = orderEventProducer.publishOrderCreated(event);

        assertThat(future).isCompleted();
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getOccurredAt()).isNotNull();
        assertThat(event.getEventType()).isEqualTo(OrderCreatedEvent.EVENT_TYPE);

        ArgumentCaptor<OrderCreatedEvent> captor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(kafkaTemplate).send(eq(KafkaTopicConfig.ORDER_CREATED_TOPIC), eq(orderId.toString()), captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("publishOrderConfirmed sends event to order.confirmed topic")
    void publishOrderConfirmed_Success() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        OrderConfirmedEvent event = OrderConfirmedEvent.builder()
                .orderId(orderId)
                .customerId(customerId)
                .build();

        when(kafkaTemplate.send(eq(KafkaTopicConfig.ORDER_CONFIRMED_TOPIC), eq(orderId.toString()), any()))
                .thenReturn(mockSuccessFuture(KafkaTopicConfig.ORDER_CONFIRMED_TOPIC, orderId.toString(), event));

        CompletableFuture<SendResult<String, Object>> future = orderEventProducer.publishOrderConfirmed(event);

        assertThat(future).isCompleted();
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getOccurredAt()).isNotNull();
        assertThat(event.getEventType()).isEqualTo(OrderConfirmedEvent.EVENT_TYPE);

        verify(kafkaTemplate).send(eq(KafkaTopicConfig.ORDER_CONFIRMED_TOPIC), eq(orderId.toString()), eq(event));
    }

    @Test
    @DisplayName("publishOrderCancelled sends event to order.cancelled topic")
    void publishOrderCancelled_Success() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(orderId)
                .customerId(customerId)
                .reason("Inventory reservation failed")
                .build();

        when(kafkaTemplate.send(eq(KafkaTopicConfig.ORDER_CANCELLED_TOPIC), eq(orderId.toString()), any()))
                .thenReturn(mockSuccessFuture(KafkaTopicConfig.ORDER_CANCELLED_TOPIC, orderId.toString(), event));

        CompletableFuture<SendResult<String, Object>> future = orderEventProducer.publishOrderCancelled(event);

        assertThat(future).isCompleted();
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getOccurredAt()).isNotNull();
        assertThat(event.getEventType()).isEqualTo(OrderCancelledEvent.EVENT_TYPE);

        verify(kafkaTemplate).send(eq(KafkaTopicConfig.ORDER_CANCELLED_TOPIC), eq(orderId.toString()), eq(event));
    }
}
