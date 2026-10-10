package com.eventdriven.order.config;

import com.eventdriven.events.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String ORDER_CREATED_TOPIC = KafkaTopics.ORDER_CREATED;
    public static final String ORDER_CONFIRMED_TOPIC = KafkaTopics.ORDER_CONFIRMED;
    public static final String ORDER_CANCELLED_TOPIC = KafkaTopics.ORDER_CANCELLED;
    public static final String INVENTORY_RESERVED_TOPIC = KafkaTopics.INVENTORY_RESERVED;
    public static final String RESERVATION_FAILED_TOPIC = KafkaTopics.INVENTORY_RESERVATION_FAILED;
    public static final String PAYMENT_COMPLETED_TOPIC = KafkaTopics.PAYMENT_COMPLETED;
    public static final String PAYMENT_FAILED_TOPIC = KafkaTopics.PAYMENT_FAILED;

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(ORDER_CREATED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderConfirmedTopic() {
        return TopicBuilder.name(ORDER_CONFIRMED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderCancelledTopic() {
        return TopicBuilder.name(ORDER_CANCELLED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryReservedTopic() {
        return TopicBuilder.name(INVENTORY_RESERVED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic reservationFailedTopic() {
        return TopicBuilder.name(RESERVATION_FAILED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
