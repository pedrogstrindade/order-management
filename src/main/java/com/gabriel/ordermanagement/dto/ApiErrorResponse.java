package com.gabriel.ordermanagement.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Standard API error response")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiErrorResponse(

        @Schema(
                description = "HTTP status code",
                example = "400"
        )
        int status,

        @Schema(
                description = "HTTP error description",
                example = "Bad Request"
        )
        String error,

        @Schema(
                description = "Human-readable error message",
                example = "Request validation failed"
        )
        String message,

        @Schema(
                description = "Field-specific validation errors"
        )
        Map<String, String> fieldErrors

) {
    public ApiErrorResponse(
            int status,
            String error,
            String message
    ) {
        this(status, error, message, Map.of());
    }
}