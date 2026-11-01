package com.gabriel.ordermanagement.controller;

import com.gabriel.ordermanagement.dto.OrderResponse;
import com.gabriel.ordermanagement.dto.PageResponse;
import com.gabriel.ordermanagement.enums.OrderStatus;
import com.gabriel.ordermanagement.exception.GlobalExceptionHandler;
import com.gabriel.ordermanagement.exception.InvalidOrderStatusTransitionException;
import com.gabriel.ordermanagement.exception.OrderNotFoundException;
import com.gabriel.ordermanagement.service.OrderService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrderControllerTest {

    private OrderService orderService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);

        LocalValidatorFactoryBean validator =
                new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new OrderController(orderService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldCreateOrderAndReturnCreated() throws Exception {
        OrderResponse response = createOrderResponse();

        when(orderService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "productId": 1,
                                      "quantity": 2
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.total").value(498));

        verify(orderService).create(any());
    }

    @Test
    void shouldRejectOrderWithoutItems() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.items")
                        .value("Order must contain at least one item"));

        verifyNoInteractions(orderService);
    }

    @Test
    void shouldRejectOrderWithInvalidQuantity() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "productId": 1,
                                      "quantity": 0
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath(
                        "$.fieldErrors['items[0].quantity']"
                ).value("Quantity must be at least 1"));

        verifyNoInteractions(orderService);
    }

    @Test
    void shouldRejectMalformedJson() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items": [
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Malformed or invalid JSON request body"));

        verifyNoInteractions(orderService);
    }

    @Test
    void shouldFindOrderByIdAndReturnOk() throws Exception {
        when(orderService.findById(1L))
                .thenReturn(createOrderResponse());

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(orderService).findById(1L);
    }

    @Test
    void shouldReturnNotFoundWhenOrderDoesNotExist()
            throws Exception {

        when(orderService.findById(999L))
                .thenThrow(new OrderNotFoundException(999L));

        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Order not found with ID: 999"))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());

        verify(orderService).findById(999L);
    }

    @Test
    void shouldFindAllOrdersAndReturnOk() throws Exception {
        PageResponse<OrderResponse> pageResponse =
                new PageResponse<>(
                        List.of(),
                        0,
                        20,
                        0,
                        0,
                        true,
                        true
                );

        when(orderService.findAll(0, 20))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/orders")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));

        verify(orderService).findAll(0, 20);
    }

    @Test
    void shouldRejectInvalidPagination() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .param("page", "-1")
                        .param("size", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Invalid pagination parameters"));

        verifyNoInteractions(orderService);
    }

    @Test
    void shouldUpdateOrderStatus() throws Exception {
        OrderResponse response = new OrderResponse(
                1L,
                OrderStatus.CONFIRMED,
                OffsetDateTime.now(),
                List.of(),
                BigDecimal.ZERO
        );

        when(orderService.updateStatus(1L, OrderStatus.CONFIRMED))
                .thenReturn(response);

        mockMvc.perform(patch("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "CONFIRMED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        verify(orderService)
                .updateStatus(1L, OrderStatus.CONFIRMED);
    }

    @Test
    void shouldRejectMissingOrderStatus() throws Exception {
        mockMvc.perform(patch("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.status")
                        .value("Order status is required"));

        verifyNoInteractions(orderService);
    }

    @Test
    void shouldRejectUnknownOrderStatus() throws Exception {
        mockMvc.perform(patch("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "PROCESSING"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Malformed or invalid JSON request body"));

        verifyNoInteractions(orderService);
    }

    @Test
    void shouldReturnConflictForInvalidStatusTransition()
            throws Exception {

        when(orderService.updateStatus(
                eq(1L),
                eq(OrderStatus.CANCELLED)
        )).thenThrow(
                new InvalidOrderStatusTransitionException(
                        OrderStatus.COMPLETED,
                        OrderStatus.CANCELLED
                )
        );

        mockMvc.perform(patch("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "CANCELLED"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(
                        "Cannot change order status from COMPLETED to CANCELLED"
                ));

        verify(orderService)
                .updateStatus(1L, OrderStatus.CANCELLED);
    }

    private OrderResponse createOrderResponse() {
        return new OrderResponse(
                1L,
                OrderStatus.PENDING,
                OffsetDateTime.now(),
                List.of(),
                new BigDecimal("498.00")
        );
    }
}