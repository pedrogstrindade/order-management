package com.gabriel.ordermanagement.dto;

import com.gabriel.ordermanagement.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(

        @NotNull(message = "Order status is required")
        OrderStatus status

) {
}