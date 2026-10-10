package com.eventdriven.events.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationEvent {

    public static final String EVENT_TYPE = "SEND_NOTIFICATION";

    private UUID eventId;

    @Builder.Default
    private String eventType = EVENT_TYPE;

    private Instant occurredAt;
    private String recipientEmail;
    private String templateKey;
    private Map<String, String> templateVars;
}
