package com.gabriel.ordermanagement.repository;

import com.gabriel.ordermanagement.dto.OrderResponse;
import com.gabriel.ordermanagement.dto.PageResponse;
import com.gabriel.ordermanagement.entity.Order;
import com.gabriel.ordermanagement.entity.OrderItem;
import com.gabriel.ordermanagement.entity.Product;
import com.gabriel.ordermanagement.enums.OrderStatus;
import com.gabriel.ordermanagement.service.OrderService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
@TestPropertySource(properties = {
        "spring.docker.compose.enabled=false",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
class OrderRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void shouldLoadOrderWithItemsAndProducts() {
        Product product = createProduct(
                "Mechanical Keyboard",
                "249.00"
        );

        Order order = createOrder(product, 2L);

        entityManager.flush();
        entityManager.clear();

        Order loadedOrder = orderRepository
                .findWithItemsById(order.getId())
                .orElseThrow();

        assertEquals(1, loadedOrder.getOrderItems().size());

        OrderItem item = loadedOrder.getOrderItems().getFirst();

        assertEquals(
                product.getId(),
                item.getProduct().getId()
        );

        assertEquals(
                "Mechanical Keyboard",
                item.getProduct().getName()
        );

        assertEquals(2L, item.getQuantity());

        assertEquals(
                0,
                new BigDecimal("249.00")
                        .compareTo(item.getPrice())
        );
    }

    @Test
    void shouldPaginateOrderIdsInDescendingOrder() {
        Product product = createProduct(
                "Mouse",
                "100.00"
        );

        Order first = createOrder(product, 1L);
        Order second = createOrder(product, 1L);
        Order third = createOrder(product, 1L);

        entityManager.flush();
        entityManager.clear();

        Page<Long> firstPage = orderRepository.findPageIds(
                PageRequest.of(0, 2)
        );

        Page<Long> secondPage = orderRepository.findPageIds(
                PageRequest.of(1, 2)
        );

        assertEquals(
                List.of(third.getId(), second.getId()),
                firstPage.getContent()
        );

        assertEquals(
                List.of(first.getId()),
                secondPage.getContent()
        );

        assertEquals(3, firstPage.getTotalElements());
        assertEquals(2, firstPage.getTotalPages());

        assertTrue(firstPage.isFirst());
        assertTrue(secondPage.isLast());
    }

    @Test
    void shouldFetchPaginatedOrdersWithoutNPlusOne() {
        Product keyboard = createProduct(
                "Keyboard",
                "249.00"
        );

        Product mouse = createProduct(
                "Mouse",
                "150.00"
        );

        Order first = new Order(OrderStatus.PENDING);

        first.addOrderItem(new OrderItem(
                keyboard.getPrice(),
                keyboard,
                first,
                2L
        ));

        first.addOrderItem(new OrderItem(
                mouse.getPrice(),
                mouse,
                first,
                1L
        ));

        orderRepository.saveAndFlush(first);

        Order second = createOrder(keyboard, 1L);
        Order third = createOrder(mouse, 3L);

        entityManager.flush();
        entityManager.clear();

        Statistics statistics = entityManagerFactory
                .unwrap(SessionFactory.class)
                .getStatistics();

        statistics.clear();

        // First page: two most recent orders.
        PageResponse<OrderResponse> firstPage =
                orderService.findAll(0, 2);

        long queryCount =
                statistics.getPrepareStatementCount();

        assertEquals(2, firstPage.content().size());
        assertEquals(3, firstPage.totalElements());
        assertEquals(2, firstPage.totalPages());

        assertEquals(0, firstPage.page());
        assertEquals(2, firstPage.size());

        assertTrue(firstPage.first());
        assertFalse(firstPage.last());

        // Orders must be returned in descending ID order.
        assertEquals(
                third.getId(),
                firstPage.content().get(0).id()
        );

        assertEquals(
                second.getId(),
                firstPage.content().get(1).id()
        );

        // The first page must not trigger N+1 queries.
        assertTrue(
                queryCount <= 3,
                "Expected at most 3 SQL statements, but executed "
                        + queryCount
        );

        // Second page: the oldest order with two items.
        PageResponse<OrderResponse> secondPage =
                orderService.findAll(1, 2);

        assertEquals(1, secondPage.content().size());

        assertEquals(
                first.getId(),
                secondPage.content().getFirst().id()
        );

        assertEquals(
                2,
                secondPage.content().getFirst().items().size()
        );

        assertEquals(
                0,
                new BigDecimal("648.00").compareTo(
                        secondPage.content().getFirst().total()
                )
        );

        assertFalse(secondPage.first());
        assertTrue(secondPage.last());
    }

    private Product createProduct(
            String name,
            String price
    ) {
        Product product = new Product(
                name,
                "Integration test product",
                new BigDecimal(price)
        );

        return productRepository.saveAndFlush(product);
    }

    private Order createOrder(
            Product product,
            Long quantity
    ) {
        Order order = new Order(OrderStatus.PENDING);

        order.addOrderItem(new OrderItem(
                product.getPrice(),
                product,
                order,
                quantity
        ));

        return orderRepository.saveAndFlush(order);
    }
}