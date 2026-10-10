package com.eventdriven.inventory.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String INVENTORY_RESERVED_TOPIC = "inventory.reserved";
    public static final String RESERVATION_FAILED_TOPIC = "inventory.reservation.failed";
    public static final String INVENTORY_RELEASED_TOPIC = "inventory.released";

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

    @Bean
    public NewTopic inventoryReleasedTopic() {
        return TopicBuilder.name(INVENTORY_RELEASED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
