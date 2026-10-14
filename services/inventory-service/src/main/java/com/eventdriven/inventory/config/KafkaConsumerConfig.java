package com.eventdriven.inventory.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Resilient Kafka consumer error handling configuration:
 * Retries transient processing failures up to 3 times with 1-second backoff.
 * Non-retryable deserialization errors and exhausted retries are logged to prevent consumer partition deadlocks.
 */
@Slf4j
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public CommonErrorHandler kafkaErrorHandler() {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, exception) -> log.error(
                        "Poison-pill or unrecoverable event on topic: {}, partition: {}, offset: {}. Skipping record. Cause: {}",
                        record.topic(), record.partition(), record.offset(), exception.getMessage(), exception),
                new FixedBackOff(1000L, 3L)
        );
        errorHandler.addNotRetryableExceptions(
                org.springframework.kafka.support.serializer.DeserializationException.class
        );
        return errorHandler;
    }
}
