package com.eventdriven.payment.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private static final int MAX_RETRY_COUNT = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Scheduled(fixedDelayString = "${outbox.poll.interval:500}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Found {} pending outbox events in payment-service", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                Object payloadToSend = deserializePayload(event);
                kafkaTemplate.send(event.getEventType(), event.getAggregateId(), payloadToSend)
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                event.setStatus(OutboxStatus.PUBLISHED);
                                event.setPublishedAt(OffsetDateTime.now());
                                log.debug("Published outbox event {} to topic {}", event.getId(), event.getEventType());
                            } else {
                                int retries = event.getRetryCount() + 1;
                                event.setRetryCount(retries);
                                if (retries >= MAX_RETRY_COUNT) {
                                    event.setStatus(OutboxStatus.FAILED);
                                    log.error("Outbox event {} exceeded max retries. Marked as FAILED.", event.getId());
                                }
                            }
                            outboxEventRepository.save(event);
                        });
            } catch (Exception e) {
                log.error("Failed to relay outbox event {}", event.getId(), e);
                int retries = event.getRetryCount() + 1;
                event.setRetryCount(retries);
                if (retries >= MAX_RETRY_COUNT) {
                    event.setStatus(OutboxStatus.FAILED);
                }
                outboxEventRepository.save(event);
            }
        }
    }

    private Object deserializePayload(OutboxEvent event) {
        if (event.getPayloadClass() != null && !event.getPayloadClass().isBlank()) {
            try {
                Class<?> clazz = Class.forName(event.getPayloadClass());
                return objectMapper.readValue(event.getPayload(), clazz);
            } catch (Exception e) {
                log.warn("Could not deserialize to class {}, sending raw payload string: {}",
                        event.getPayloadClass(), e.getMessage());
            }
        }
        return event.getPayload();
    }
}
