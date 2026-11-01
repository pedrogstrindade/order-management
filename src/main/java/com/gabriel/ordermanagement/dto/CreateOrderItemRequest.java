package com.gabriel.ordermanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Product and quantity requested in an order")
public record CreateOrderItemRequest(

        @Schema(
                description = "Identifier of an existing product",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Product ID is required")
        Long productId,

        @Schema(
                description = "Quantity of the product",
                example = "2",
                minimum = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Long quantity
) {
}