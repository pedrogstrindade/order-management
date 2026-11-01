package com.gabriel.ordermanagement.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderStatusTest {

    @Test
    void shouldAllowValidTransitions() {
        assertTrue(OrderStatus.PENDING.canTransitionTo(
                OrderStatus.CONFIRMED
        ));

        assertTrue(OrderStatus.PENDING.canTransitionTo(
                OrderStatus.CANCELLED
        ));

        assertTrue(OrderStatus.CONFIRMED.canTransitionTo(
                OrderStatus.COMPLETED
        ));

        assertTrue(OrderStatus.CONFIRMED.canTransitionTo(
                OrderStatus.CANCELLED
        ));
    }

    @Test
    void shouldRejectInvalidTransitions() {
        assertFalse(OrderStatus.PENDING.canTransitionTo(
                OrderStatus.COMPLETED
        ));

        assertFalse(OrderStatus.CONFIRMED.canTransitionTo(
                OrderStatus.PENDING
        ));

        assertFalse(OrderStatus.COMPLETED.canTransitionTo(
                OrderStatus.CANCELLED
        ));

        assertFalse(OrderStatus.CANCELLED.canTransitionTo(
                OrderStatus.CONFIRMED
        ));
    }

    @Test
    void shouldRejectSameStatusAndNull() {
        for (OrderStatus status : OrderStatus.values()) {
            assertFalse(status.canTransitionTo(status));
            assertFalse(status.canTransitionTo(null));
        }
    }
}