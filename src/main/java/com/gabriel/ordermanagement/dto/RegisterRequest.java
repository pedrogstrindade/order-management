package com.gabriel.ordermanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "User registration request")
public record RegisterRequest(

        @Schema(example = "user@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        String email,

        @Schema(example = "MySecurePassword123!")
        @NotBlank(message = "Password is required")
        @Size(
                min = 12,
                max = 72,
                message = "Password must contain between 12 and 72 characters"
        )
        String password
) {
}