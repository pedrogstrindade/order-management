package com.gabriel.ordermanagement.repository;

import com.gabriel.ordermanagement.dto.CreateOrderItemRequest;
import com.gabriel.ordermanagement.dto.CreateOrderRequest;
import com.gabriel.ordermanagement.dto.OrderResponse;
import com.gabriel.ordermanagement.entity.Order;
import com.gabriel.ordermanagement.entity.Product;
import com.gabriel.ordermanagement.enums.OrderStatus;
import com.gabriel.ordermanagement.service.OrderService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class OrderStatusIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void shouldPersistOrderStatusAndIncrementVersion() {
        Long orderId = createPendingOrder();

        Order initialOrder = orderRepository.findById(orderId)
                .orElseThrow();

        Long initialVersion = initialOrder.getVersion();

        assertEquals(
                OrderStatus.PENDING,
                initialOrder.getOrderStatus()
        );

        OrderResponse response = orderService.updateStatus(
                orderId,
                OrderStatus.CONFIRMED
        );

        assertEquals(OrderStatus.CONFIRMED, response.status());

        Order updatedOrder = orderRepository.findById(orderId)
                .orElseThrow();

        assertEquals(
                OrderStatus.CONFIRMED,
                updatedOrder.getOrderStatus()
        );

        assertEquals(
                initialVersion + 1,
                updatedOrder.getVersion()
        );
    }

    @Test
    void shouldRejectUpdateUsingStaleVersion() {
        Long orderId = createPendingOrder();

        EntityManager firstManager =
                entityManagerFactory.createEntityManager();

        EntityManager secondManager =
                entityManagerFactory.createEntityManager();

        try {
            firstManager.getTransaction().begin();
            secondManager.getTransaction().begin();

            Order firstOrder = firstManager.find(
                    Order.class,
                    orderId
            );

            Order secondOrder = secondManager.find(
                    Order.class,
                    orderId
            );

            assertEquals(
                    firstOrder.getVersion(),
                    secondOrder.getVersion()
            );

            firstOrder.setOrderStatus(OrderStatus.CONFIRMED);

            firstManager.getTransaction().commit();

            secondOrder.setOrderStatus(OrderStatus.CANCELLED);

            assertThrows(
                    OptimisticLockException.class,
                    secondManager::flush
            );

        } finally {
            if (firstManager.getTransaction().isActive()) {
                firstManager.getTransaction().rollback();
            }

            if (secondManager.getTransaction().isActive()) {
                secondManager.getTransaction().rollback();
            }

            firstManager.close();
            secondManager.close();
        }

        Order persistedOrder = orderRepository.findById(orderId)
                .orElseThrow();

        assertEquals(
                OrderStatus.CONFIRMED,
                persistedOrder.getOrderStatus()
        );
    }

    private Long createPendingOrder() {
        Product product = productRepository.save(
                new Product(
                        "Integration Test Product",
                        "Product for optimistic locking tests",
                        new BigDecimal("99.90")
                )
        );

        OrderResponse response = orderService.create(
                new CreateOrderRequest(
                        List.of(
                                new CreateOrderItemRequest(
                                        product.getId(),
                                        1L
                                )
                        )
                )
        );

        return response.id();
    }
}