package com.gabriel.ordermanagement.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "Request to create an order")
public record CreateOrderRequest(

        @ArraySchema(
                arraySchema = @Schema(
                        description = "Products and quantities included in the order",
                        requiredMode = Schema.RequiredMode.REQUIRED
                ),
                minItems = 1,
                schema = @Schema(
                        implementation = CreateOrderItemRequest.class
                )
        )
        @NotEmpty(message = "Order must contain at least one item")
        List<@Valid CreateOrderItemRequest> items

) {
}