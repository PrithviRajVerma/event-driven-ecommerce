package com.eventdriven.notification.service;

import com.eventdriven.events.order.OrderCancelledEvent;
import com.eventdriven.events.order.OrderCreatedEvent;
import com.eventdriven.events.payment.PaymentCompletedEvent;
import com.eventdriven.events.payment.PaymentFailedEvent;
import com.eventdriven.notification.domain.Notification;
import com.eventdriven.notification.domain.NotificationStatus;
import com.eventdriven.notification.domain.NotificationType;
import com.eventdriven.notification.dto.NotificationResponse;
import com.eventdriven.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("handleOrderCreated sends email and saves Notification record")
    void handleOrderCreated_Success() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(orderId)
                .customerId(customerId)
                .totalAmount(new BigDecimal("120.00"))
                .items(List.of())
                .build();

        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        Notification result = notificationService.handleOrderCreated(event);

        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getCustomerId()).isEqualTo(customerId);
        assertThat(result.getNotificationType()).isEqualTo(NotificationType.ORDER_CREATED);
        assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);

        verify(emailService).sendEmail(contains("customer-" + customerId), contains(orderId.toString()), anyString());
    }

    @Test
    @DisplayName("handlePaymentCompleted sends receipt email and saves record")
    void handlePaymentCompleted_Success() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .customerId(customerId)
                .amount(new BigDecimal("99.99"))
                .currency("USD")
                .occurredAt(Instant.now())
                .build();

        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        Notification result = notificationService.handlePaymentCompleted(event);

        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getNotificationType()).isEqualTo(NotificationType.PAYMENT_COMPLETED);

        verify(emailService).sendEmail(contains("customer-" + customerId), contains("Payment Receipt"), anyString());
    }

    @Test
    @DisplayName("handlePaymentFailed sends alert email and saves record")
    void handlePaymentFailed_Success() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .orderId(orderId)
                .customerId(customerId)
                .amount(new BigDecimal("500.00"))
                .reason("Insufficient funds")
                .occurredAt(Instant.now())
                .build();

        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        Notification result = notificationService.handlePaymentFailed(event);

        assertThat(result).isNotNull();
        assertThat(result.getNotificationType()).isEqualTo(NotificationType.PAYMENT_FAILED);

        verify(emailService).sendEmail(contains("customer-" + customerId), contains("Payment Failed"), anyString());
    }

    @Test
    @DisplayName("handleOrderCancelled sends cancellation email and saves record")
    void handleOrderCancelled_Success() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(orderId)
                .customerId(customerId)
                .reason("Payment authorization timed out")
                .build();

        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        Notification result = notificationService.handleOrderCancelled(event);

        assertThat(result).isNotNull();
        assertThat(result.getNotificationType()).isEqualTo(NotificationType.ORDER_CANCELLED);

        verify(emailService).sendEmail(contains("customer-" + customerId), contains("Order Cancelled"), anyString());
    }

    @Test
    @DisplayName("getCustomerNotifications returns mapped responses")
    void getCustomerNotifications_Success() {
        UUID customerId = UUID.randomUUID();
        Notification n = Notification.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .orderId(UUID.randomUUID())
                .recipientEmail("customer@test.com")
                .notificationType(NotificationType.ORDER_CREATED)
                .subject("Sub")
                .content("Content")
                .status(NotificationStatus.SENT)
                .createdAt(OffsetDateTime.now())
                .build();

        when(notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)).thenReturn(List.of(n));

        List<NotificationResponse> responses = notificationService.getCustomerNotifications(customerId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getCustomerId()).isEqualTo(customerId);
    }
}
