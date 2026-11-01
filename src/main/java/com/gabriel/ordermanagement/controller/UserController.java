package com.gabriel.ordermanagement.controller;

import com.gabriel.ordermanagement.dto.UserResponse;
import com.gabriel.ordermanagement.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Authenticated user operations")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get current authenticated user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<UserResponse> me(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                authService.findCurrentUser(jwt)
        );
    }
}