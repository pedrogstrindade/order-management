package com.gabriel.ordermanagement.dto;

import com.gabriel.ordermanagement.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to update an order status")
public record UpdateOrderStatusRequest(

        @Schema(
                description = """
                        Target order status.

                        Allowed transitions:
                        PENDING to CONFIRMED or CANCELLED;
                        CONFIRMED to COMPLETED or CANCELLED.

                        COMPLETED and CANCELLED are terminal.
                        """,
                example = "CONFIRMED",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Order status is required")
        OrderStatus status
) {
}