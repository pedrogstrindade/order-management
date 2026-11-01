package com.gabriel.ordermanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Schema(description = "Request to create or update a product")
public record CreateProductRequest(

        @Schema(
                description = "Product name",
                example = "Mechanical Keyboard",
                maxLength = 150,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Product name is required")
        @Size(max = 150, message = "Product name must not exceed 150 characters")
        String name,

        @Schema(
                description = "Optional product description",
                example = "Mechanical keyboard with RGB lighting",
                maxLength = 500
        )
        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @Schema(
                description = "Product unit price",
                example = "249.90",
                minimum = "0",
                maximum = "9999999999.99",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Product price is required")
        @DecimalMax(value = "9999999999.99", message = "Product price must not exceed 10 integer digits")
        @DecimalMin(value = "0.00", message = "Product price must not be negative")
        @Digits(integer = 10, fraction = 2, message = "Product price must have at most 2 decimal places")
        BigDecimal price
) {
}