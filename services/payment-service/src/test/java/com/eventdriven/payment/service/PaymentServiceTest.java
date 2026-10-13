package com.eventdriven.payment.service;

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
import com.eventdriven.payment.producer.PaymentEventProducer;
import com.eventdriven.payment.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("Successfully processes payment and publishes PaymentCompletedEvent")
    void processPayment_Success() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("150.00");

        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .orderId(orderId)
                .customerId(customerId)
                .amount(amount)
                .currency("USD")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            p.setCreatedAt(OffsetDateTime.now());
            p.setUpdatedAt(OffsetDateTime.now());
            return p;
        });

        PaymentResponse response = paymentService.processPayment(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(response.getOrderId()).isEqualTo(orderId);
        assertThat(response.getAmount()).isEqualTo(amount);
        assertThat(response.getTransactionReference()).startsWith("TXN-");

        ArgumentCaptor<PaymentCompletedEvent> captor = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(paymentEventProducer).publishPaymentCompleted(captor.capture());
        PaymentCompletedEvent publishedEvent = captor.getValue();
        assertThat(publishedEvent.getOrderId()).isEqualTo(orderId);
        assertThat(publishedEvent.getCustomerId()).isEqualTo(customerId);
        assertThat(publishedEvent.getAmount()).isEqualTo(amount);
        verify(paymentEventProducer, never()).publishPaymentFailed(any());
    }

    @Test
    @DisplayName("Payment exceeding transaction limit fails and publishes PaymentFailedEvent")
    void processPayment_ExceedsLimit_Fails() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        BigDecimal excessiveAmount = new BigDecimal("25000.00");

        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .orderId(orderId)
                .customerId(customerId)
                .amount(excessiveAmount)
                .currency("USD")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            p.setCreatedAt(OffsetDateTime.now());
            p.setUpdatedAt(OffsetDateTime.now());
            return p;
        });

        PaymentResponse response = paymentService.processPayment(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(response.getFailureReason()).contains("Transaction limit exceeded");

        ArgumentCaptor<PaymentFailedEvent> captor = ArgumentCaptor.forClass(PaymentFailedEvent.class);
        verify(paymentEventProducer).publishPaymentFailed(captor.capture());
        PaymentFailedEvent publishedEvent = captor.getValue();
        assertThat(publishedEvent.getOrderId()).isEqualTo(orderId);
        assertThat(publishedEvent.getCustomerId()).isEqualTo(customerId);
        assertThat(publishedEvent.getReason()).contains("Transaction limit exceeded");
        verify(paymentEventProducer, never()).publishPaymentCompleted(any());
    }

    @Test
    @DisplayName("Throws InvalidPaymentOperationException when payment amount is zero or negative")
    void processPayment_ZeroOrNegativeAmount_ThrowsException() {
        ProcessPaymentRequest zeroRequest = ProcessPaymentRequest.builder()
                .orderId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .amount(BigDecimal.ZERO)
                .build();

        assertThatThrownBy(() -> paymentService.processPayment(zeroRequest))
                .isInstanceOf(InvalidPaymentOperationException.class)
                .hasMessageContaining("greater than zero");

        verifyNoInteractions(paymentRepository);
        verifyNoInteractions(paymentEventProducer);
    }

    @Test
    @DisplayName("Idempotently returns existing completed payment without re-processing")
    void processPayment_ExistingCompleted_ReturnsIdempotently() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Payment existing = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .customerId(customerId)
                .amount(new BigDecimal("99.99"))
                .currency("USD")
                .status(PaymentStatus.COMPLETED)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .transactionReference("TXN-EXISTING")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(existing));

        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .orderId(orderId)
                .customerId(customerId)
                .amount(new BigDecimal("99.99"))
                .build();

        PaymentResponse response = paymentService.processPayment(request);

        assertThat(response.getId()).isEqualTo(existing.getId());
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(paymentEventProducer);
    }

    @Test
    @DisplayName("Processes order payment via processOrderPayment")
    void processOrderPayment_Success() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("49.50");

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        PaymentResponse response = paymentService.processOrderPayment(orderId, customerId, amount, "USD");

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        verify(paymentEventProducer).publishPaymentCompleted(any());
    }

    @Test
    @DisplayName("Retrieves payment by order ID and customer ID")
    void getPaymentByOrderId_Success() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .customerId(customerId)
                .amount(new BigDecimal("80.00"))
                .currency("USD")
                .status(PaymentStatus.COMPLETED)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();

        when(paymentRepository.findByOrderIdAndCustomerId(orderId, customerId)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getPaymentByOrderId(orderId, customerId);

        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("Throws PaymentNotFoundException when order payment is not found")
    void getPaymentByOrderId_NotFound_ThrowsException() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(paymentRepository.findByOrderIdAndCustomerId(orderId, customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentByOrderId(orderId, customerId))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    @DisplayName("Throws UnauthorizedPaymentAccessException when customerId is null")
    void getPaymentByOrderId_NullCustomerId_ThrowsUnauthorized() {
        assertThatThrownBy(() -> paymentService.getPaymentByOrderId(UUID.randomUUID(), null))
                .isInstanceOf(UnauthorizedPaymentAccessException.class);
    }

    @Test
    @DisplayName("Lists all payments for customer ordered by created date")
    void getCustomerPayments_Success() {
        UUID customerId = UUID.randomUUID();
        Payment payment1 = Payment.builder().id(UUID.randomUUID()).customerId(customerId).orderId(UUID.randomUUID()).amount(new BigDecimal("10.00")).status(PaymentStatus.COMPLETED).paymentMethod(PaymentMethod.CREDIT_CARD).build();
        Payment payment2 = Payment.builder().id(UUID.randomUUID()).customerId(customerId).orderId(UUID.randomUUID()).amount(new BigDecimal("20.00")).status(PaymentStatus.COMPLETED).paymentMethod(PaymentMethod.CREDIT_CARD).build();

        when(paymentRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)).thenReturn(List.of(payment1, payment2));

        List<PaymentResponse> responses = paymentService.getCustomerPayments(customerId);

        assertThat(responses).hasSize(2);
    }
}
