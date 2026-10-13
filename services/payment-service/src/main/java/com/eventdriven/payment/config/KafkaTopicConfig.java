package com.eventdriven.payment.config;

import com.eventdriven.events.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String PAYMENT_COMPLETED_TOPIC = KafkaTopics.PAYMENT_COMPLETED;
    public static final String PAYMENT_FAILED_TOPIC = KafkaTopics.PAYMENT_FAILED;
    public static final String PAYMENT_INITIATED_TOPIC = KafkaTopics.PAYMENT_INITIATED;
    public static final String INVENTORY_RESERVED_TOPIC = KafkaTopics.INVENTORY_RESERVED;

    @Bean
    public NewTopic paymentCompletedTopic() {
        return TopicBuilder.name(PAYMENT_COMPLETED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentFailedTopic() {
        return TopicBuilder.name(PAYMENT_FAILED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentInitiatedTopic() {
        return TopicBuilder.name(PAYMENT_INITIATED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
