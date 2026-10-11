package com.gabriel.ordermanagement.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateProductRequest(

        @NotBlank(message = "Product name is required")
        @Size(max = 150, message = "Product name must not exceed 150 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @NotNull(message = "Product price is required")
        @DecimalMax(value = "9999999999.99", message = "Product price must not exceed 10 integer digits")
        @DecimalMin(value = "0.00", message = "Product price must not be negative")
        @Digits(integer = 10, fraction = 2, message = "Product price must have at most 2 decimal places")
        BigDecimal price) {
}