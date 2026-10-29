package com.gabriel.ordermanagement.service;

import com.gabriel.ordermanagement.dto.CreateOrderItemRequest;
import com.gabriel.ordermanagement.dto.CreateOrderRequest;
import com.gabriel.ordermanagement.dto.OrderResponse;
import com.gabriel.ordermanagement.dto.PageResponse;
import com.gabriel.ordermanagement.entity.Order;
import com.gabriel.ordermanagement.entity.OrderItem;
import com.gabriel.ordermanagement.entity.Product;
import com.gabriel.ordermanagement.enums.OrderStatus;
import com.gabriel.ordermanagement.exception.OrderNotFoundException;
import com.gabriel.ordermanagement.exception.ProductNotFoundException;
import com.gabriel.ordermanagement.repository.OrderRepository;
import com.gabriel.ordermanagement.repository.ProductRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
        assertEquals(2L, response.items().getFirst().quantity());
        assertEquals(
                "Mechanical Keyboard",
                response.items().getFirst().productName()
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
        OrderItem savedItem = savedOrder.getOrderItems().getFirst();

        product.setPrice(new BigDecimal("399.00"));

        assertEquals(
                0,
                new BigDecimal("249.00").compareTo(savedItem.getPrice())
        );
        assertSame(savedOrder, savedItem.getOrder());
    }

    @Test
    void shouldFindOrderById() {
        Order order = createOrderWithId(1L);

        when(orderRepository.findWithItemsById(1L))
                .thenReturn(Optional.of(order));

        OrderResponse response = orderService.findById(1L);

        assertEquals(1L, response.id());
        assertEquals(OrderStatus.PENDING, response.status());
        assertNotNull(response.createdAt());

        verify(orderRepository).findWithItemsById(1L);
    }

    @Test
    void shouldThrowExceptionWhenOrderDoesNotExist() {
        when(orderRepository.findWithItemsById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                OrderNotFoundException.class,
                () -> orderService.findById(999L)
        );

        verify(orderRepository).findWithItemsById(999L);
    }

    @Test
    void shouldFindAllOrders() {
        Order firstOrder = createOrderWithId(2L);
        Order secondOrder = createOrderWithId(1L);

        Pageable pageable = PageRequest.of(0, 20);

        Page<Long> idPage = new PageImpl<>(
                List.of(2L, 1L),
                pageable,
                2
        );

        when(orderRepository.findPageIds(pageable))
                .thenReturn(idPage);

        when(orderRepository.findAllWithItemsByIdIn(
                List.of(2L, 1L)
        )).thenReturn(List.of(firstOrder, secondOrder));

        PageResponse<OrderResponse> response =
                orderService.findAll(0, 20);

        assertEquals(2, response.content().size());
        assertEquals(2, response.totalElements());
        assertEquals(1, response.totalPages());
        assertEquals(0, response.page());
        assertEquals(20, response.size());

        assertEquals(2L, response.content().get(0).id());
        assertEquals(1L, response.content().get(1).id());

        verify(orderRepository).findPageIds(pageable);
        verify(orderRepository).findAllWithItemsByIdIn(
                List.of(2L, 1L)
        );
    }

    @Test
    void shouldReturnEmptyPageWhenNoOrdersExist() {
        Pageable pageable = PageRequest.of(0, 20);

        Page<Long> emptyPage = new PageImpl<>(
                List.of(),
                pageable,
                0
        );

        when(orderRepository.findPageIds(pageable))
                .thenReturn(emptyPage);

        PageResponse<OrderResponse> response =
                orderService.findAll(0, 20);

        assertTrue(response.content().isEmpty());
        assertEquals(0, response.totalElements());
        assertEquals(0, response.totalPages());
        assertTrue(response.first());
        assertTrue(response.last());

        verify(orderRepository).findPageIds(pageable);
        verify(orderRepository, never())
                .findAllWithItemsByIdIn(anyList());
    }

    @Test
    void shouldReturnCorrectPageMetadata() {
        Pageable pageable = PageRequest.of(1, 2);

        Page<Long> idPage = new PageImpl<>(
                List.of(3L, 2L),
                pageable,
                5
        );

        Order firstOrder = createOrderWithId(3L);
        Order secondOrder = createOrderWithId(2L);

        when(orderRepository.findPageIds(pageable))
                .thenReturn(idPage);

        when(orderRepository.findAllWithItemsByIdIn(
                List.of(3L, 2L)
        )).thenReturn(List.of(firstOrder, secondOrder));

        PageResponse<OrderResponse> response =
                orderService.findAll(1, 2);

        assertEquals(1, response.page());
        assertEquals(2, response.size());
        assertEquals(5, response.totalElements());
        assertEquals(3, response.totalPages());
        assertFalse(response.first());
        assertFalse(response.last());
        assertEquals(2, response.content().size());
    }

    @Test
    void shouldPreservePaginationOrder() {
        Pageable pageable = PageRequest.of(0, 3);

        Page<Long> idPage = new PageImpl<>(
                List.of(3L, 2L, 1L),
                pageable,
                3
        );

        Order first = createOrderWithId(1L);
        Order second = createOrderWithId(2L);
        Order third = createOrderWithId(3L);

        when(orderRepository.findPageIds(pageable))
                .thenReturn(idPage);

        when(orderRepository.findAllWithItemsByIdIn(
                List.of(3L, 2L, 1L)
        )).thenReturn(List.of(first, second, third));

        PageResponse<OrderResponse> response =
                orderService.findAll(0, 3);

        assertEquals(3L, response.content().get(0).id());
        assertEquals(2L, response.content().get(1).id());
        assertEquals(1L, response.content().get(2).id());

        verify(orderRepository).findPageIds(pageable);
        verify(orderRepository).findAllWithItemsByIdIn(
                List.of(3L, 2L, 1L)
        );
    }

    private Order createOrderWithId(Long id) {
        Order order = new Order(OrderStatus.PENDING);
        ReflectionTestUtils.setField(order, "id", id);
        return order;
    }
}