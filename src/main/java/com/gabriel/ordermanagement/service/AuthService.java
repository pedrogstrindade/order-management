package com.gabriel.ordermanagement.service;

import com.gabriel.ordermanagement.dto.AuthResponse;
import com.gabriel.ordermanagement.dto.LoginRequest;
import com.gabriel.ordermanagement.dto.RegisterRequest;
import com.gabriel.ordermanagement.dto.UserResponse;
import com.gabriel.ordermanagement.entity.User;
import com.gabriel.ordermanagement.enums.Role;
import com.gabriel.ordermanagement.exception.EmailAlreadyRegisteredException;
import com.gabriel.ordermanagement.exception.InvalidCredentialsException;
import com.gabriel.ordermanagement.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class AuthService {

    private static final Duration ACCESS_TOKEN_TTL =
            Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final String issuer;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            Clock clock,
            @Value("${app.jwt.issuer}") String issuer
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.issuer = issuer;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        User user = new User(
                email,
                passwordEncoder.encode(request.password()),
                Role.USER
        );

        try {
            return UserResponse.from(
                    userRepository.saveAndFlush(user)
            );
        } catch (DataIntegrityViolationException exception) {
            if (userRepository.existsByEmail(email)) {
                throw new EmailAlreadyRegisteredException();
            }

            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isEnabled()
                || !passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            throw new InvalidCredentialsException();
        }

        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(ACCESS_TOKEN_TTL);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("roles", List.of(user.getRole().name()))
                .build();

        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(claims)
        ).getTokenValue();

        return new AuthResponse(
                token,
                "Bearer",
                ACCESS_TOKEN_TTL.toSeconds()
        );
    }

    @Transactional(readOnly = true)
    public UserResponse findCurrentUser(Jwt jwt) {
        Long userId;

        try {
            userId = Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isEnabled()) {
            throw new InvalidCredentialsException();
        }

        return UserResponse.from(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}