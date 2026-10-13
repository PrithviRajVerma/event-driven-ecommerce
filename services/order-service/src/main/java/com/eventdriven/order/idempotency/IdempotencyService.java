package com.eventdriven.order.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final ProcessedEventRepository processedEventRepository;

    @Transactional(readOnly = true)
    public boolean isAlreadyProcessed(UUID eventId) {
        if (eventId == null) {
            return false;
        }
        return processedEventRepository.existsById(eventId);
    }

    @Transactional
    public void markAsProcessed(UUID eventId, String eventType, String consumerGroup) {
        if (eventId == null) {
            return;
        }
        ProcessedEvent processedEvent = ProcessedEvent.builder()
                .eventId(eventId)
                .eventType(eventType != null ? eventType : "UNKNOWN")
                .consumerGroup(consumerGroup != null ? consumerGroup : "order-service-group")
                .processedAt(OffsetDateTime.now())
                .build();

        processedEventRepository.save(processedEvent);
        log.debug("Marked event {} as processed for group {}", eventId, consumerGroup);
    }
}
