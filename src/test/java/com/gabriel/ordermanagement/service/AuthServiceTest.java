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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtEncoder jwtEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtEncoder = mock(JwtEncoder.class);

        Clock clock = Clock.fixed(
                Instant.parse("2026-11-01T12:00:00Z"),
                ZoneOffset.UTC
        );

        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtEncoder,
                clock
        );
    }

    @Test
    void shouldRegisterUserWithNormalizedEmailAndEncodedPassword() {
        when(userRepository.existsByEmail("user@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("SecurePassword123!"))
                .thenReturn("encoded-password");

        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.register(
                new RegisterRequest(
                        "  User@Example.COM  ",
                        "SecurePassword123!"
                )
        );

        assertEquals("user@example.com", response.email());
        assertEquals(Role.USER, response.role());
        assertTrue(response.enabled());

        verify(passwordEncoder).encode("SecurePassword123!");

        verify(userRepository).saveAndFlush(argThat(user ->
                user.getEmail().equals("user@example.com")
                        && user.getPasswordHash().equals("encoded-password")
                        && user.getRole() == Role.USER
        ));
    }

    @Test
    void shouldRejectDuplicateEmail() {
        when(userRepository.existsByEmail("user@example.com"))
                .thenReturn(true);

        assertThrows(
                EmailAlreadyRegisteredException.class,
                () -> authService.register(
                        new RegisterRequest(
                                "User@Example.com",
                                "SecurePassword123!"
                        )
                )
        );

        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldLoginWithValidCredentials() {
        User user = new User(
                "user@example.com",
                "stored-hash",
                Role.USER
        );

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "SecurePassword123!",
                "stored-hash"
        )).thenReturn(true);

        Jwt jwt = new Jwt(
                "signed-token",
                Instant.parse("2026-11-01T12:00:00Z"),
                Instant.parse("2026-11-01T12:15:00Z"),
                Map.of("alg", "RS256"),
                Map.of("sub", "1")
        );

        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(jwt);

        // A persisted entity normally has an assigned ID.
        // This test uses a mocked entity to provide one.
        User persistedUser = mock(User.class);
        when(persistedUser.getId()).thenReturn(1L);
        when(persistedUser.getRole()).thenReturn(Role.USER);
        when(persistedUser.getEmail()).thenReturn("user@example.com");
        when(persistedUser.getPasswordHash()).thenReturn("stored-hash");
        when(persistedUser.isEnabled()).thenReturn(true);

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(persistedUser));

        AuthResponse response = authService.login(
                new LoginRequest(
                        "User@Example.COM",
                        "SecurePassword123!"
                )
        );

        assertEquals("signed-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900, response.expiresIn());

        verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
    }

    @Test
    void shouldRejectUnknownEmail() {
        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(
                        new LoginRequest(
                                "missing@example.com",
                                "SecurePassword123!"
                        )
                )
        );

        verifyNoInteractions(jwtEncoder);
    }

    @Test
    void shouldRejectIncorrectPassword() {
        User user = new User(
                "user@example.com",
                "stored-hash",
                Role.USER
        );

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("wrong-password", "stored-hash"))
                .thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(
                        new LoginRequest(
                                "user@example.com",
                                "wrong-password"
                        )
                )
        );

        verifyNoInteractions(jwtEncoder);
    }

    @Test
    void shouldRejectDisabledUser() {
        User user = mock(User.class);

        when(user.isEnabled()).thenReturn(false);

        when(userRepository.findByEmail("disabled@example.com"))
                .thenReturn(Optional.of(user));

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(
                        new LoginRequest(
                                "disabled@example.com",
                                "SecurePassword123!"
                        )
                )
        );

        verifyNoInteractions(jwtEncoder);
    }

    @Test
    void shouldReturnCurrentUser() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getEmail()).thenReturn("user@example.com");
        when(user.getRole()).thenReturn(Role.USER);
        when(user.isEnabled()).thenReturn(true);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        Jwt jwt = new Jwt(
                "token",
                Instant.parse("2026-11-01T12:00:00Z"),
                Instant.parse("2026-11-01T12:15:00Z"),
                Map.of("alg", "RS256"),
                Map.of("sub", "1")
        );

        UserResponse response = authService.findCurrentUser(jwt);

        assertEquals(1L, response.id());
        assertEquals("user@example.com", response.email());
        assertEquals(Role.USER, response.role());
    }
}