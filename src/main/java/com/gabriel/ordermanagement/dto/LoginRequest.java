package com.gabriel.ordermanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "User login credentials")
public record LoginRequest(

        @Schema(example = "user@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @Schema(example = "MySecurePassword123!")
        @NotBlank(message = "Password is required")
        String password
) {
}