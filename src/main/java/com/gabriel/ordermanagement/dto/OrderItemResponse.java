package com.gabriel.ordermanagement.dto;

import com.gabriel.ordermanagement.entity.OrderItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Item included in an order")
public record OrderItemResponse(

        @Schema(
                description = "Unique order item identifier",
                example = "1"
        )
        Long id,

        @Schema(
                description = "Referenced product identifier",
                example = "1"
        )
        Long productId,

        @Schema(
                description = "Product name",
                example = "Mechanical Keyboard"
        )
        String productName,

        @Schema(
                description = "Quantity ordered",
                example = "2"
        )
        Long quantity,

        @Schema(
                description = "Product unit price captured when the order was created",
                example = "249.90"
        )
        BigDecimal unitPrice,

        @Schema(
                description = "Item subtotal: unit price multiplied by quantity",
                example = "499.80"
        )
        BigDecimal subtotal
) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getPrice(),
                item.getPrice().multiply(
                        BigDecimal.valueOf(item.getQuantity())
                )
        );
    }
}