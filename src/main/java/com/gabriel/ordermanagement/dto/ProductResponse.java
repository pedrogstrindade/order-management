package com.gabriel.ordermanagement.dto;

import com.gabriel.ordermanagement.entity.Product;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "Product details")
public record ProductResponse(

        @Schema(
                description = "Unique product identifier",
                example = "1"
        )
        Long id,

        @Schema(
                description = "Product name",
                example = "Mechanical Keyboard"
        )
        String name,

        @Schema(
                description = "Product description",
                example = "Mechanical keyboard with RGB lighting"
        )
        String description,

        @Schema(
                description = "Current product unit price",
                example = "249.90"
        )
        BigDecimal price,

        @Schema(
                description = "Product creation timestamp",
                example = "2026-10-28T21:30:00-03:00"
        )
        OffsetDateTime createdAt
) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCreatedAt()
        );
    }
}