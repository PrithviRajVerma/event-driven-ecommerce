package com.eventdriven.inventory.producer;

import com.eventdriven.events.inventory.InventoryReleasedEvent;
import com.eventdriven.events.inventory.InventoryReservedEvent;
import com.eventdriven.events.inventory.ReservationFailedEvent;
import com.eventdriven.inventory.config.KafkaTopicConfig;
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
class InventoryEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private InventoryEventProducer inventoryEventProducer;

    @BeforeEach
    void setUp() {
        inventoryEventProducer = new InventoryEventProducer(kafkaTemplate);
    }

    private CompletableFuture<SendResult<String, Object>> mockSuccessFuture(String topic, String key, Object payload) {
        ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(topic, key, payload);
        RecordMetadata metadata = new RecordMetadata(new TopicPartition(topic, 0), 0, 0, 0, 0, 0);
        SendResult<String, Object> sendResult = new SendResult<>(producerRecord, metadata);
        return CompletableFuture.completedFuture(sendResult);
    }

    @Test
    @DisplayName("publishInventoryReserved sends event to inventory.reserved topic with orderId key")
    void publishInventoryReserved_Success() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        InventoryReservedEvent event = InventoryReservedEvent.builder()
                .orderId(orderId)
                .customerId(customerId)
                .totalAmount(new BigDecimal("150.00"))
                .reservedItems(List.of(
                        InventoryReservedEvent.ReservedItem.builder()
                                .productId(productId)
                                .quantity(3)
                                .build()
                ))
                .build();

        when(kafkaTemplate.send(eq(KafkaTopicConfig.INVENTORY_RESERVED_TOPIC), eq(orderId.toString()), any()))
                .thenReturn(mockSuccessFuture(KafkaTopicConfig.INVENTORY_RESERVED_TOPIC, orderId.toString(), event));

        CompletableFuture<SendResult<String, Object>> future = inventoryEventProducer.publishInventoryReserved(event);

        assertThat(future).isCompleted();
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getOccurredAt()).isNotNull();
        assertThat(event.getEventType()).isEqualTo(InventoryReservedEvent.EVENT_TYPE);

        ArgumentCaptor<InventoryReservedEvent> captor = ArgumentCaptor.forClass(InventoryReservedEvent.class);
        verify(kafkaTemplate).send(eq(KafkaTopicConfig.INVENTORY_RESERVED_TOPIC), eq(orderId.toString()), captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("publishReservationFailed sends event to inventory.reservation.failed topic")
    void publishReservationFailed_Success() {
        UUID orderId = UUID.randomUUID();

        ReservationFailedEvent event = ReservationFailedEvent.builder()
                .orderId(orderId)
                .reason("Insufficient stock for product")
                .build();

        when(kafkaTemplate.send(eq(KafkaTopicConfig.RESERVATION_FAILED_TOPIC), eq(orderId.toString()), any()))
                .thenReturn(mockSuccessFuture(KafkaTopicConfig.RESERVATION_FAILED_TOPIC, orderId.toString(), event));

        CompletableFuture<SendResult<String, Object>> future = inventoryEventProducer.publishReservationFailed(event);

        assertThat(future).isCompleted();
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getOccurredAt()).isNotNull();
        assertThat(event.getEventType()).isEqualTo(ReservationFailedEvent.EVENT_TYPE);

        verify(kafkaTemplate).send(eq(KafkaTopicConfig.RESERVATION_FAILED_TOPIC), eq(orderId.toString()), eq(event));
    }

    @Test
    @DisplayName("publishInventoryReleased sends event to inventory.released topic")
    void publishInventoryReleased_Success() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        InventoryReleasedEvent event = InventoryReleasedEvent.builder()
                .orderId(orderId)
                .releasedItems(List.of(
                        InventoryReleasedEvent.ReleasedItem.builder()
                                .productId(productId)
                                .quantity(2)
                                .build()
                ))
                .build();

        when(kafkaTemplate.send(eq(KafkaTopicConfig.INVENTORY_RELEASED_TOPIC), eq(orderId.toString()), any()))
                .thenReturn(mockSuccessFuture(KafkaTopicConfig.INVENTORY_RELEASED_TOPIC, orderId.toString(), event));

        CompletableFuture<SendResult<String, Object>> future = inventoryEventProducer.publishInventoryReleased(event);

        assertThat(future).isCompleted();
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getOccurredAt()).isNotNull();
        assertThat(event.getEventType()).isEqualTo(InventoryReleasedEvent.EVENT_TYPE);

        verify(kafkaTemplate).send(eq(KafkaTopicConfig.INVENTORY_RELEASED_TOPIC), eq(orderId.toString()), eq(event));
    }
}
