package com.gabriel.ordermanagement.enums;

import java.util.Set;

public enum OrderStatus {

    PENDING,
    CONFIRMED,
    COMPLETED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus target) {
        if (target == null) {
            return false;
        }

        return switch (this) {
            case PENDING -> Set.of(
                    CONFIRMED, CANCELLED
            ).contains(target);

            case CONFIRMED -> Set.of(
                    COMPLETED, CANCELLED
            ).contains(target);

            case COMPLETED, CANCELLED -> false;
        };
    }
}