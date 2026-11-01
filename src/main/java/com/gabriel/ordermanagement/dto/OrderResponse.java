package com.gabriel.ordermanagement.dto;

import com.gabriel.ordermanagement.entity.Order;
import com.gabriel.ordermanagement.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Order details including items and total amount")
public record OrderResponse(

        @Schema(
                description = "Unique order identifier",
                example = "1"
        )
        Long id,

        @Schema(
                description = "Current order status",
                example = "PENDING"
        )
        OrderStatus status,

        @Schema(
                description = "Order creation timestamp",
                example = "2026-10-28T21:54:26-03:00"
        )
        OffsetDateTime createdAt,

        @Schema(
                description = "Items included in the order"
        )
        List<OrderItemResponse> items,

        @Schema(
                description = "Total order amount calculated from item subtotals",
                example = "499.80"
        )
        BigDecimal total
) {

    public static OrderResponse from(Order order) {
        List<OrderItemResponse> items = order.getOrderItems()
                .stream()
                .map(OrderItemResponse::from)
                .toList();

        BigDecimal total = items.stream()
                .map(OrderItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new OrderResponse(
                order.getId(),
                order.getOrderStatus(),
                order.getCreatedAt(),
                items,
                total
        );
    }
}