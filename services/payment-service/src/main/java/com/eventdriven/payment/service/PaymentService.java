package com.eventdriven.payment.service;

import com.eventdriven.events.KafkaTopics;
import com.eventdriven.events.payment.PaymentCompletedEvent;
import com.eventdriven.events.payment.PaymentFailedEvent;
import com.eventdriven.payment.domain.Payment;
import com.eventdriven.payment.domain.PaymentMethod;
import com.eventdriven.payment.domain.PaymentStatus;
import com.eventdriven.payment.dto.PaymentResponse;
import com.eventdriven.payment.dto.ProcessPaymentRequest;
import com.eventdriven.payment.exception.InvalidPaymentOperationException;
import com.eventdriven.payment.exception.PaymentNotFoundException;
import com.eventdriven.payment.exception.UnauthorizedPaymentAccessException;
import com.eventdriven.payment.outbox.OutboxService;
import com.eventdriven.payment.producer.PaymentEventProducer;
import com.eventdriven.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final BigDecimal TRANSACTION_LIMIT = new BigDecimal("10000.00");

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer paymentEventProducer;
    private final OutboxService outboxService;

    @Transactional
    public PaymentResponse processPayment(ProcessPaymentRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentOperationException("Payment amount must be greater than zero");
        }

        UUID orderId = request.getOrderId();
        UUID customerId = request.getCustomerId();

        // Check for existing payment on this order
        Optional<Payment> existingPaymentOpt = paymentRepository.findByOrderId(orderId);
        if (existingPaymentOpt.isPresent()) {
            Payment existing = existingPaymentOpt.get();
            if (existing.getStatus() == PaymentStatus.COMPLETED) {
                log.info("Payment for order {} already completed with id {}", orderId, existing.getId());
                return mapToResponse(existing);
            }
        }

        Payment payment = existingPaymentOpt.orElseGet(() -> Payment.builder()
                .orderId(orderId)
                .customerId(customerId)
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.PENDING)
                .build());

        payment.setAmount(request.getAmount());
        if (request.getCurrency() != null) {
            payment.setCurrency(request.getCurrency());
        }
        if (request.getPaymentMethod() != null) {
            payment.setPaymentMethod(request.getPaymentMethod());
        }

        // Simulate payment gateway authorization
        if (request.getAmount().compareTo(TRANSACTION_LIMIT) > 0) {
            String failureReason = "Payment declined: Transaction limit exceeded ($" + TRANSACTION_LIMIT + " max)";
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(failureReason);
            Payment saved = paymentRepository.save(payment);

            PaymentFailedEvent failedEvent = PaymentFailedEvent.builder()
                    .orderId(orderId)
                    .customerId(customerId)
                    .amount(request.getAmount())
                    .reason(failureReason)
                    .occurredAt(Instant.now())
                    .build();

            paymentEventProducer.publishPaymentFailed(failedEvent);
            outboxService.saveEvent("PAYMENT", saved.getId().toString(), KafkaTopics.PAYMENT_FAILED, failedEvent);
            log.warn("Payment failed for order {}: {}", orderId, failureReason);
            return mapToResponse(saved);
        }

        // Successful payment execution
        String transactionReference = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionReference(transactionReference);
        payment.setFailureReason(null);
        Payment saved = paymentRepository.save(payment);

        PaymentCompletedEvent completedEvent = PaymentCompletedEvent.builder()
                .orderId(orderId)
                .customerId(customerId)
                .paymentId(saved.getId())
                .amount(saved.getAmount())
                .currency(saved.getCurrency())
                .occurredAt(Instant.now())
                .build();

        paymentEventProducer.publishPaymentCompleted(completedEvent);
        outboxService.saveEvent("PAYMENT", saved.getId().toString(), KafkaTopics.PAYMENT_COMPLETED, completedEvent);
        log.info("Payment completed successfully for order {}. PaymentId: {}, TxnRef: {}",
                orderId, saved.getId(), transactionReference);

        return mapToResponse(saved);
    }

    @Transactional
    public PaymentResponse processOrderPayment(UUID orderId, UUID customerId, BigDecimal amount, String currency) {
        if (orderId == null) {
            throw new InvalidPaymentOperationException("Order ID cannot be null");
        }

        Optional<Payment> existingOpt = paymentRepository.findByOrderId(orderId);
        if (existingOpt.isPresent()) {
            Payment existing = existingOpt.get();
            if (existing.getStatus() == PaymentStatus.COMPLETED) {
                log.info("Payment already completed for order {}", orderId);
                return mapToResponse(existing);
            }
        }

        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .orderId(orderId)
                .customerId(customerId)
                .amount(amount != null ? amount : BigDecimal.ZERO)
                .currency(currency != null ? currency : "USD")
                .paymentMethod(PaymentMethod.MOCK_GATEWAY)
                .build();

        return processPayment(request);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(UUID orderId, UUID customerId) {
        if (customerId == null) {
            throw new UnauthorizedPaymentAccessException("Authentication required to view payment");
        }

        Payment payment = paymentRepository.findByOrderIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for order id: " + orderId));

        return mapToResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UUID id, UUID customerId) {
        if (customerId == null) {
            throw new UnauthorizedPaymentAccessException("Authentication required to view payment");
        }

        Payment payment = paymentRepository.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new PaymentNotFoundException(id));

        return mapToResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getCustomerPayments(UUID customerId) {
        if (customerId == null) {
            throw new UnauthorizedPaymentAccessException("Authentication required to view payment history");
        }

        return paymentRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .customerId(payment.getCustomerId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .transactionReference(payment.getTransactionReference())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
