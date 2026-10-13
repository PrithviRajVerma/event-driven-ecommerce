package com.eventdriven.notification.e2e;

import com.eventdriven.events.order.OrderCancelledEvent;
import com.eventdriven.events.order.OrderCreatedEvent;
import com.eventdriven.events.payment.PaymentCompletedEvent;
import com.eventdriven.events.payment.PaymentFailedEvent;
import com.eventdriven.notification.consumer.NotificationEventConsumer;
import com.eventdriven.notification.domain.Notification;
import com.eventdriven.notification.domain.NotificationStatus;
import com.eventdriven.notification.domain.NotificationType;
import com.eventdriven.notification.idempotency.IdempotencyService;
import com.eventdriven.notification.repository.NotificationRepository;
import com.eventdriven.notification.service.EmailService;
import com.eventdriven.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * End-to-End Notification Saga Test.
 *
 * Verifies that the Notification Service correctly reacts to all saga lifecycle events:
 * 1. OrderCreatedEvent ➔ dispatches order confirmation email & logs audit record
 * 2. PaymentCompletedEvent ➔ dispatches payment receipt email & logs audit record
 * 3. PaymentFailedEvent ➔ dispatches payment failure alert email & logs audit record
 * 4. OrderCancelledEvent ➔ dispatches order cancellation notice email & logs audit record
 * 5. Deduplication: duplicate saga events are skipped without re-sending emails
 */
@ExtendWith(MockitoExtension.class)
class EndToEndNotificationSagaTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private IdempotencyService idempotencyService;

    private NotificationService notificationService;
    private NotificationEventConsumer consumer;

    private UUID orderId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, emailService);
        consumer = new NotificationEventConsumer(notificationService, idempotencyService);

        orderId = UUID.randomUUID();
        customerId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Saga Step: OrderCreatedEvent triggers email dispatch and persistent notification audit")
    void testOrderCreated_FullNotificationFlow() {
        UUID eventId = UUID.randomUUID();
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .customerId(customerId)
                .totalAmount(new BigDecimal("99.95"))
                .items(List.of())
                .build();

        when(idempotencyService.isAlreadyProcessed(eventId)).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        consumer.handleOrderCreated(event);

        // Verify email dispatched
        verify(emailService).sendEmail(
                eq("customer-" + customerId + "@ecommerce.com"),
                contains("Order Confirmation"),
                contains("$99.95")
        );

        // Verify notification saved with SENT status
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getOrderId()).isEqualTo(orderId);
        assertThat(saved.getCustomerId()).isEqualTo(customerId);
        assertThat(saved.getNotificationType()).isEqualTo(NotificationType.ORDER_CREATED);
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.SENT);

        // Verify idempotency recorded
        verify(idempotencyService).markAsProcessed(eq(eventId), eq(event.getEventType()), eq("notification-service-group"));
    }

    @Test
    @DisplayName("Saga Step: PaymentCompletedEvent triggers receipt email and audit record")
    void testPaymentCompleted_FullNotificationFlow() {
        UUID eventId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .paymentId(paymentId)
                .customerId(customerId)
                .amount(new BigDecimal("99.95"))
                .currency("USD")
                .occurredAt(Instant.now())
                .build();

        when(idempotencyService.isAlreadyProcessed(eventId)).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        consumer.handlePaymentCompleted(event);

        verify(emailService).sendEmail(
                eq("customer-" + customerId + "@ecommerce.com"),
                contains("Payment Receipt"),
                contains("$99.95")
        );

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getNotificationType()).isEqualTo(NotificationType.PAYMENT_COMPLETED);
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.SENT);

        verify(idempotencyService).markAsProcessed(eq(eventId), eq(event.getEventType()), eq("notification-service-group"));
    }

    @Test
    @DisplayName("Saga Compensation: PaymentFailedEvent triggers alert email and audit record")
    void testPaymentFailed_FullNotificationFlow() {
        UUID eventId = UUID.randomUUID();
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .customerId(customerId)
                .amount(new BigDecimal("99.95"))
                .reason("Card expired")
                .occurredAt(Instant.now())
                .build();

        when(idempotencyService.isAlreadyProcessed(eventId)).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        consumer.handlePaymentFailed(event);

        verify(emailService).sendEmail(
                eq("customer-" + customerId + "@ecommerce.com"),
                contains("Payment Failed"),
                contains("Card expired")
        );

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getNotificationType()).isEqualTo(NotificationType.PAYMENT_FAILED);
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.SENT);

        verify(idempotencyService).markAsProcessed(eq(eventId), eq(event.getEventType()), eq("notification-service-group"));
    }

    @Test
    @DisplayName("Saga Compensation: OrderCancelledEvent triggers cancellation notice and audit record")
    void testOrderCancelled_FullNotificationFlow() {
        UUID eventId = UUID.randomUUID();
        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .eventId(eventId)
                .orderId(orderId)
                .customerId(customerId)
                .reason("Out of stock")
                .build();

        when(idempotencyService.isAlreadyProcessed(eventId)).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        consumer.handleOrderCancelled(event);

        verify(emailService).sendEmail(
                eq("customer-" + customerId + "@ecommerce.com"),
                contains("Order Cancelled"),
                contains("Out of stock")
        );

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getNotificationType()).isEqualTo(NotificationType.ORDER_CANCELLED);
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.SENT);

        verify(idempotencyService).markAsProcessed(eq(eventId), eq(event.getEventType()), eq("notification-service-group"));
    }

    @Test
    @DisplayName("Idempotency Guard: Duplicate event is rejected without sending duplicate emails")
    void testDuplicateEvent_IsSkipped() {
        UUID dupEventId = UUID.randomUUID();
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(dupEventId)
                .orderId(orderId)
                .customerId(customerId)
                .totalAmount(new BigDecimal("50.00"))
                .build();

        when(idempotencyService.isAlreadyProcessed(dupEventId)).thenReturn(true);

        consumer.handleOrderCreated(event);

        verifyNoInteractions(emailService);
        verifyNoInteractions(notificationRepository);
    }
}
