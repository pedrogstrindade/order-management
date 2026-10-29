package com.gabriel.ordermanagement.dto;

import com.gabriel.ordermanagement.entity.Order;
import com.gabriel.ordermanagement.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        OrderStatus status,
        OffsetDateTime createdAt,
        List<OrderItemResponse> items,
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