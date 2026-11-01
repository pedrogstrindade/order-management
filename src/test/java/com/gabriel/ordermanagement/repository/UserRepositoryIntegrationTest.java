package com.gabriel.ordermanagement.repository;

import com.gabriel.ordermanagement.entity.Order;
import com.gabriel.ordermanagement.entity.User;
import com.gabriel.ordermanagement.enums.OrderStatus;
import com.gabriel.ordermanagement.enums.Role;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class UserRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void shouldPersistAndFindUserWithNormalizedEmail() {
        String email =
                "User-" + UUID.randomUUID() + "@Example.COM";

        User saved = userRepository.saveAndFlush(
                new User(
                        "  " + email + "  ",
                        "bcrypt-hash-placeholder",
                        Role.USER
                )
        );

        User found = userRepository
                .findByEmail(email.toLowerCase())
                .orElseThrow();

        assertEquals(saved.getId(), found.getId());
        assertEquals(Role.USER, found.getRole());
        assertTrue(found.isEnabled());
        assertNotNull(found.getCreatedAt());

        assertEquals(
                "bcrypt-hash-placeholder",
                found.getPasswordHash()
        );
    }

    @Test
    void shouldRejectDuplicateEmail() {
        String email =
                "unique-" + UUID.randomUUID() + "@example.com";

        userRepository.saveAndFlush(
                new User(email, "hash-one", Role.USER)
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(
                        new User(email, "hash-two", Role.USER)
                )
        );
    }

    @Test
    void shouldPersistOrderOwnershipAndAllowLegacyOrders() {
        User user = userRepository.saveAndFlush(
                new User(
                        "owner-" + UUID.randomUUID() + "@example.com",
                        "hash",
                        Role.USER
                )
        );

        Order owned = new Order(OrderStatus.PENDING);
        owned.setUser(user);

        Long ownedId = orderRepository
                .saveAndFlush(owned)
                .getId();

        Order loaded = orderRepository
                .findById(ownedId)
                .orElseThrow();

        assertEquals(
                user.getId(),
                loaded.getUser().getId()
        );

        Order legacy = orderRepository.saveAndFlush(
                new Order(OrderStatus.PENDING)
        );

        assertNull(
                orderRepository
                        .findById(legacy.getId())
                        .orElseThrow()
                        .getUser()
        );
    }
}