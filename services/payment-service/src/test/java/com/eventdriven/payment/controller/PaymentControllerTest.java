package com.eventdriven.payment.controller;

import com.eventdriven.payment.domain.PaymentMethod;
import com.eventdriven.payment.domain.PaymentStatus;
import com.eventdriven.payment.dto.PaymentResponse;
import com.eventdriven.payment.dto.ProcessPaymentRequest;
import com.eventdriven.payment.exception.GlobalExceptionHandler;
import com.eventdriven.payment.exception.PaymentNotFoundException;
import com.eventdriven.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private UUID customerId;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        customerId = UUID.randomUUID();
        auth = new UsernamePasswordAuthenticationToken(
                customerId,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    @DisplayName("POST /api/v1/payments/process - Successfully processes payment")
    void processPayment_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .orderId(orderId)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .customerId(customerId)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status(PaymentStatus.COMPLETED)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .transactionReference("TXN-12345678")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(paymentService.processPayment(any(ProcessPaymentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/process")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.transactionReference").value("TXN-12345678"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/order/{orderId} - Successfully retrieves payment for order")
    void getPaymentByOrderId_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        PaymentResponse response = PaymentResponse.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .customerId(customerId)
                .amount(new BigDecimal("50.00"))
                .status(PaymentStatus.COMPLETED)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();

        when(paymentService.getPaymentByOrderId(eq(orderId), eq(customerId))).thenReturn(response);

        mockMvc.perform(get("/api/v1/payments/order/{orderId}", orderId)
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/order/{orderId} - Returns 404 when payment not found")
    void getPaymentByOrderId_NotFound() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(paymentService.getPaymentByOrderId(eq(orderId), eq(customerId)))
                .thenThrow(new PaymentNotFoundException("Payment not found"));

        mockMvc.perform(get("/api/v1/payments/order/{orderId}", orderId)
                        .principal(auth))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PAYMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/{id} - Successfully retrieves payment by ID")
    void getPaymentById_Success() throws Exception {
        UUID paymentId = UUID.randomUUID();
        PaymentResponse response = PaymentResponse.builder()
                .id(paymentId)
                .orderId(UUID.randomUUID())
                .customerId(customerId)
                .amount(new BigDecimal("75.00"))
                .status(PaymentStatus.COMPLETED)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();

        when(paymentService.getPaymentById(eq(paymentId), eq(customerId))).thenReturn(response);

        mockMvc.perform(get("/api/v1/payments/{id}", paymentId)
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/payments - Returns customer payment history")
    void getCustomerPayments_Success() throws Exception {
        PaymentResponse p1 = PaymentResponse.builder().id(UUID.randomUUID()).customerId(customerId).amount(new BigDecimal("20.00")).status(PaymentStatus.COMPLETED).paymentMethod(PaymentMethod.CREDIT_CARD).build();
        PaymentResponse p2 = PaymentResponse.builder().id(UUID.randomUUID()).customerId(customerId).amount(new BigDecimal("35.00")).status(PaymentStatus.COMPLETED).paymentMethod(PaymentMethod.CREDIT_CARD).build();

        when(paymentService.getCustomerPayments(customerId)).thenReturn(List.of(p1, p2));

        mockMvc.perform(get("/api/v1/payments")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
