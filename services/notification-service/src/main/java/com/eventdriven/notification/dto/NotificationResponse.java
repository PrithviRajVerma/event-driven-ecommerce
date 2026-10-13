package com.eventdriven.notification.dto;

import com.eventdriven.notification.domain.NotificationStatus;
import com.eventdriven.notification.domain.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private UUID id;
    private UUID orderId;
    private UUID customerId;
    private String recipientEmail;
    private NotificationType notificationType;
    private String subject;
    private String content;
    private NotificationStatus status;
    private OffsetDateTime sentAt;
    private OffsetDateTime createdAt;
}
