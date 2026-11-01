package com.gabriel.ordermanagement.dto;

import com.gabriel.ordermanagement.entity.User;
import com.gabriel.ordermanagement.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Public user information")
public record UserResponse(

        Long id,
        String email,
        Role role,
        boolean enabled,
        OffsetDateTime createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}