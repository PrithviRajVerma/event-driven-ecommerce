package com.eventdriven.notification.controller;

import com.eventdriven.notification.domain.NotificationStatus;
import com.eventdriven.notification.domain.NotificationType;
import com.eventdriven.notification.dto.NotificationResponse;
import com.eventdriven.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController).build();
    }

    @Test
    @DisplayName("GET /api/v1/notifications/customer/{customerId} returns list of notifications")
    void getCustomerNotifications_Success() throws Exception {
        UUID customerId = UUID.randomUUID();
        NotificationResponse response = NotificationResponse.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .orderId(UUID.randomUUID())
                .recipientEmail("cust@test.com")
                .notificationType(NotificationType.ORDER_CREATED)
                .subject("Order Confirmation")
                .content("Details")
                .status(NotificationStatus.SENT)
                .createdAt(OffsetDateTime.now())
                .build();

        when(notificationService.getCustomerNotifications(customerId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/notifications/customer/{customerId}", customerId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value(customerId.toString()))
                .andExpect(jsonPath("$[0].notificationType").value("ORDER_CREATED"));
    }

    @Test
    @DisplayName("GET /api/v1/notifications/order/{orderId} returns list of notifications")
    void getOrderNotifications_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        NotificationResponse response = NotificationResponse.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .customerId(UUID.randomUUID())
                .recipientEmail("cust@test.com")
                .notificationType(NotificationType.PAYMENT_COMPLETED)
                .subject("Payment Receipt")
                .content("Receipt details")
                .status(NotificationStatus.SENT)
                .createdAt(OffsetDateTime.now())
                .build();

        when(notificationService.getOrderNotifications(orderId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/notifications/order/{orderId}", orderId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(orderId.toString()))
                .andExpect(jsonPath("$[0].notificationType").value("PAYMENT_COMPLETED"));
    }
}
