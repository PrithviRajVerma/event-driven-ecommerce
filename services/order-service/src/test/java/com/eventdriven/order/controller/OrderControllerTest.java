package com.eventdriven.order.controller;

import com.eventdriven.order.dto.CancelOrderRequest;
import com.eventdriven.order.dto.CheckoutRequest;
import com.eventdriven.order.dto.OrderItemResponse;
import com.eventdriven.order.dto.OrderResponse;
import com.eventdriven.order.entity.OrderStatus;
import com.eventdriven.order.exception.GlobalExceptionHandler;
import com.eventdriven.order.exception.InvalidOrderOperationException;
import com.eventdriven.order.exception.OrderNotFoundException;
import com.eventdriven.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
class OrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private UUID customerId;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        customerId = UUID.randomUUID();
        auth = new UsernamePasswordAuthenticationToken(customerId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void checkout_authenticated_returnsCreated() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderResponse response = OrderResponse.builder()
                .id(orderId)
                .customerId(customerId)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("79.98"))
                .currency("USD")
                .shippingAddress("100 Pine St, SF")
                .items(List.of(
                        OrderItemResponse.builder()
                                .id(UUID.randomUUID())
                                .productId(UUID.randomUUID())
                                .quantity(2)
                                .unitPrice(new BigDecimal("39.99"))
                                .subtotal(new BigDecimal("79.98"))
                                .build()
                ))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(orderService.createOrderFromCart(eq(customerId), any(CheckoutRequest.class))).thenReturn(response);

        CheckoutRequest request = CheckoutRequest.builder()
                .shippingAddress("100 Pine St, SF")
                .build();

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(79.98))
                .andExpect(jsonPath("$.shippingAddress").value("100 Pine St, SF"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void checkout_unauthenticated_returnsUnauthorized() throws Exception {
        CheckoutRequest request = CheckoutRequest.builder().build();

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void checkout_emptyCart_returnsBadRequest() throws Exception {
        when(orderService.createOrderFromCart(eq(customerId), any(CheckoutRequest.class)))
                .thenThrow(new InvalidOrderOperationException("Cannot checkout with an empty cart"));

        CheckoutRequest request = CheckoutRequest.builder().build();

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_ORDER_OPERATION"))
                .andExpect(jsonPath("$.message").value("Cannot checkout with an empty cart"));
    }

    @Test
    void getOrders_authenticated_returnsList() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderResponse response = OrderResponse.builder()
                .id(orderId)
                .customerId(customerId)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("50.00"))
                .currency("USD")
                .items(List.of())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(orderService.getCustomerOrders(customerId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/orders").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(orderId.toString()))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void getOrder_authenticated_found_returnsOk() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderResponse response = OrderResponse.builder()
                .id(orderId)
                .customerId(customerId)
                .status(OrderStatus.CONFIRMED)
                .totalAmount(new BigDecimal("120.00"))
                .currency("USD")
                .items(List.of())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(orderService.getOrder(orderId, customerId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/orders/" + orderId).principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void getOrder_notFound_returnsNotFound() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.getOrder(orderId, customerId)).thenThrow(new OrderNotFoundException(orderId));

        mockMvc.perform(get("/api/v1/orders/" + orderId).principal(auth))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ORDER_NOT_FOUND"));
    }

    @Test
    void cancelOrder_authenticated_returnsOk() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderResponse response = OrderResponse.builder()
                .id(orderId)
                .customerId(customerId)
                .status(OrderStatus.CANCELLED)
                .totalAmount(new BigDecimal("120.00"))
                .currency("USD")
                .items(List.of())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(orderService.cancelOrder(orderId, customerId, "No longer needed")).thenReturn(response);

        CancelOrderRequest cancelRequest = CancelOrderRequest.builder()
                .reason("No longer needed")
                .build();

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
