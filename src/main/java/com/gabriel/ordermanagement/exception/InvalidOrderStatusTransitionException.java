package com.gabriel.ordermanagement.exception;

import com.gabriel.ordermanagement.enums.OrderStatus;

public class InvalidOrderStatusTransitionException
        extends RuntimeException {

    public InvalidOrderStatusTransitionException(
            OrderStatus current,
            OrderStatus target
    ) {
        super("Cannot change order status from "
                + current + " to " + target);
    }
}