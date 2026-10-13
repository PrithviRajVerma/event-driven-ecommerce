package com.eventdriven.inventory.outbox;

import com.eventdriven.events.inventory.InventoryReservedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private OutboxService outboxService;

    @Test
    @DisplayName("saveEvent serializes payload to JSON and saves OutboxEvent with PENDING status")
    void saveEvent_Success() {
        UUID orderId = UUID.randomUUID();
        InventoryReservedEvent event = InventoryReservedEvent.builder()
                .orderId(orderId)
                .build();

        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(i -> i.getArgument(0));

        OutboxEvent saved = outboxService.saveEvent("INVENTORY", orderId.toString(), "inventory.reserved", event);

        assertThat(saved).isNotNull();
        assertThat(saved.getAggregateType()).isEqualTo("INVENTORY");
        assertThat(saved.getAggregateId()).isEqualTo(orderId.toString());
        assertThat(saved.getEventType()).isEqualTo("inventory.reserved");
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(saved.getPayload()).contains(orderId.toString());
        assertThat(saved.getPayloadClass()).isEqualTo(InventoryReservedEvent.class.getName());

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OutboxStatus.PENDING);
    }
}
