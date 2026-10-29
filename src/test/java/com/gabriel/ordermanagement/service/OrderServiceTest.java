package com.gabriel.ordermanagement.service;

import com.gabriel.ordermanagement.dto.CreateOrderItemRequest;
import com.gabriel.ordermanagement.dto.CreateOrderRequest;
import com.gabriel.ordermanagement.dto.OrderResponse;
import com.gabriel.ordermanagement.entity.Order;
import com.gabriel.ordermanagement.entity.OrderItem;
import com.gabriel.ordermanagement.entity.Product;
import com.gabriel.ordermanagement.enums.OrderStatus;
import com.gabriel.ordermanagement.exception.ProductNotFoundException;
import com.gabriel.ordermanagement.repository.OrderRepository;
import com.gabriel.ordermanagement.repository.ProductRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void shouldCreateOrderSuccessfully() {
        Product product = new Product(
                "Mechanical Keyboard",
                "Gaming keyboard",
                new BigDecimal("249.00")
        );

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new CreateOrderItemRequest(1L, 2L))
        );

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.create(request);

        assertEquals(OrderStatus.PENDING, response.status());
        assertEquals(1, response.items().size());
        assertEquals(
                0,
                new BigDecimal("498.00").compareTo(response.total())
        );

        assertEquals(2L, response.items().get(0).quantity());
        assertEquals(
                "Mechanical Keyboard",
                response.items().get(0).productName()
        );

        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void shouldThrowExceptionWhenProductDoesNotExist() {
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new CreateOrderItemRequest(999L, 1L))
        );

        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> orderService.create(request)
        );

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void shouldCalculateTotalWithMultipleProducts() {
        Product keyboard = new Product(
                "Keyboard",
                "Mechanical keyboard",
                new BigDecimal("249.00")
        );

        Product mouse = new Product(
                "Mouse",
                "Wireless mouse",
                new BigDecimal("150.00")
        );

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(
                        new CreateOrderItemRequest(1L, 2L),
                        new CreateOrderItemRequest(2L, 3L)
                )
        );

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(keyboard));

        when(productRepository.findById(2L))
                .thenReturn(Optional.of(mouse));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.create(request);

        assertEquals(2, response.items().size());

        assertEquals(
                0,
                new BigDecimal("948.00").compareTo(response.total())
        );

        verify(productRepository).findById(1L);
        verify(productRepository).findById(2L);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void shouldPreserveProductPriceAtOrderCreation() {
        Product product = new Product(
                "Keyboard",
                "Mechanical keyboard",
                new BigDecimal("249.00")
        );

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new CreateOrderItemRequest(1L, 1L))
        );

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        orderService.create(request);

        ArgumentCaptor<Order> captor =
                ArgumentCaptor.forClass(Order.class);

        verify(orderRepository).save(captor.capture());

        Order savedOrder = captor.getValue();
        OrderItem savedItem = savedOrder.getOrderItems().get(0);

        product.setPrice(new BigDecimal("399.00"));

        assertEquals(
                0,
                new BigDecimal("249.00").compareTo(savedItem.getPrice())
        );

        assertSame(savedOrder, savedItem.getOrder());
    }
}