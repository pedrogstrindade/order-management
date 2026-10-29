package com.gabriel.ordermanagement.controller;

import com.gabriel.ordermanagement.dto.OrderResponse;
import com.gabriel.ordermanagement.enums.OrderStatus;
import com.gabriel.ordermanagement.exception.GlobalExceptionHandler;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        OrderResponse response = new OrderResponse(
                1L,
                OrderStatus.PENDING,
                OffsetDateTime.now(),
                List.of(),
                new BigDecimal("498.00")
        );

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
                .andExpect(jsonPath("$.items").exists());

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
                .andExpect(jsonPath(
                        "$['items[0].quantity']"
                ).exists());

        verifyNoInteractions(orderService);
    }

    @Test
    void shouldFindOrderByIdAndReturnOk() throws Exception {
        OrderResponse response = new OrderResponse(
                1L,
                OrderStatus.PENDING,
                OffsetDateTime.now(),
                List.of(),
                BigDecimal.ZERO
        );

        when(orderService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldReturnNotFoundWhenOrderDoesNotExist()
            throws Exception {

        when(orderService.findById(999L))
                .thenThrow(new OrderNotFoundException(999L));

        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("404"));
    }

    @Test
    void shouldFindAllOrdersAndReturnOk() throws Exception {
        when(orderService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}