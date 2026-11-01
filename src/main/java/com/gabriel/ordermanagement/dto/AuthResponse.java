package com.gabriel.ordermanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT authentication response")
public record AuthResponse(

        @Schema(description = "Signed JWT access token")
        String accessToken,

        @Schema(example = "Bearer")
        String tokenType,

        @Schema(description = "Token validity in seconds", example = "900")
        long expiresIn
) {
}