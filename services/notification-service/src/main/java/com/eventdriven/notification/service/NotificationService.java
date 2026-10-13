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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    @Transactional
    public Notification handleOrderCreated(OrderCreatedEvent event) {
        String recipient = resolveCustomerEmail(event.getCustomerId());
        String subject = "Order Confirmation - #" + event.getOrderId();
        int itemCount = event.getItems() != null ? event.getItems().size() : 0;
        String content = String.format("Thank you for your order #%s! Total: $%s. Number of items: %d.",
                event.getOrderId(), event.getTotalAmount(), itemCount);

        emailService.sendEmail(recipient, subject, content);

        Notification notification = Notification.builder()
                .orderId(event.getOrderId())
                .customerId(event.getCustomerId())
                .recipientEmail(recipient)
                .notificationType(NotificationType.ORDER_CREATED)
                .subject(subject)
                .content(content)
                .status(NotificationStatus.SENT)
                .sentAt(OffsetDateTime.now())
                .build();

        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification handlePaymentCompleted(PaymentCompletedEvent event) {
        String recipient = resolveCustomerEmail(event.getCustomerId());
        String subject = "Payment Receipt - Order #" + event.getOrderId();
        String content = String.format("Your payment of $%s for order #%s completed successfully. Transaction ID: %s.",
                event.getAmount(), event.getOrderId(), event.getPaymentId());

        emailService.sendEmail(recipient, subject, content);

        Notification notification = Notification.builder()
                .orderId(event.getOrderId())
                .customerId(event.getCustomerId())
                .recipientEmail(recipient)
                .notificationType(NotificationType.PAYMENT_COMPLETED)
                .subject(subject)
                .content(content)
                .status(NotificationStatus.SENT)
                .sentAt(OffsetDateTime.now())
                .build();

        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification handlePaymentFailed(PaymentFailedEvent event) {
        String recipient = resolveCustomerEmail(event.getCustomerId());
        String subject = "Action Required: Payment Failed for Order #" + event.getOrderId();
        String content = String.format("Payment failed for order #%s (Amount: $%s). Reason: %s. Please update payment method.",
                event.getOrderId(), event.getAmount(), event.getReason());

        emailService.sendEmail(recipient, subject, content);

        Notification notification = Notification.builder()
                .orderId(event.getOrderId())
                .customerId(event.getCustomerId())
                .recipientEmail(recipient)
                .notificationType(NotificationType.PAYMENT_FAILED)
                .subject(subject)
                .content(content)
                .status(NotificationStatus.SENT)
                .sentAt(OffsetDateTime.now())
                .build();

        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification handleOrderCancelled(OrderCancelledEvent event) {
        String recipient = resolveCustomerEmail(event.getCustomerId());
        String subject = "Order Cancelled - #" + event.getOrderId();
        String content = String.format("Order #%s has been cancelled. Reason: %s.",
                event.getOrderId(), event.getReason());

        emailService.sendEmail(recipient, subject, content);

        Notification notification = Notification.builder()
                .orderId(event.getOrderId())
                .customerId(event.getCustomerId())
                .recipientEmail(recipient)
                .notificationType(NotificationType.ORDER_CANCELLED)
                .subject(subject)
                .content(content)
                .status(NotificationStatus.SENT)
                .sentAt(OffsetDateTime.now())
                .build();

        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getCustomerNotifications(UUID customerId) {
        return notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getOrderNotifications(UUID orderId) {
        return notificationRepository.findByOrderIdOrderByCreatedAtDesc(orderId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private String resolveCustomerEmail(UUID customerId) {
        return (customerId != null ? "customer-" + customerId + "@ecommerce.com" : "customer@ecommerce.com");
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .orderId(notification.getOrderId())
                .customerId(notification.getCustomerId())
                .recipientEmail(notification.getRecipientEmail())
                .notificationType(notification.getNotificationType())
                .subject(notification.getSubject())
                .content(notification.getContent())
                .status(notification.getStatus())
                .sentAt(notification.getSentAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
