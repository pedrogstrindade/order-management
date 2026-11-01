package com.gabriel.ordermanagement.security;

import com.gabriel.ordermanagement.entity.User;
import com.gabriel.ordermanagement.enums.Role;
import com.gabriel.ordermanagement.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthIntegrationTest {

    private static final String PASSWORD = "MySecurePassword123!";

    private static final String ISSUER =
            "https://auth.order-management.example";

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Test
    void shouldRegisterLoginAndAccessCurrentUser() throws Exception {
        String email = uniqueEmail();

        String registerResponse = mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody(email, PASSWORD))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number userId = JsonPath.read(registerResponse, "$.id");

        User savedUser = userRepository.findByEmail(email)
                .orElseThrow();

        assertEquals(userId.longValue(), savedUser.getId());
        assertEquals(Role.USER, savedUser.getRole());
        assertNotEquals(PASSWORD, savedUser.getPasswordHash());

        assertTrue(
                passwordEncoder.matches(
                        PASSWORD,
                        savedUser.getPasswordHash()
                )
        );

        String loginResponse = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody(email, PASSWORD))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = JsonPath.read(loginResponse, "$.accessToken");

        var decodedJwt = jwtDecoder.decode(token);

        assertEquals(
                savedUser.getId().toString(),
                decodedJwt.getSubject()
        );

        assertEquals(
                ISSUER,
                decodedJwt.getIssuer().toString()
        );

        assertEquals(
                List.of("USER"),
                decodedJwt.getClaimAsStringList("roles")
        );

        assertEquals(
                "RS256",
                decodedJwt.getHeaders().get("alg").toString()
        );

        mockMvc.perform(
                        get("/api/users/me")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(savedUser.getId()))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void shouldRejectDuplicateRegistration() throws Exception {
        String email = uniqueEmail();

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody(email, PASSWORD))
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody(email, PASSWORD))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Email is already registered"));
    }

    @Test
    void shouldRejectIncorrectPassword() throws Exception {
        String email = uniqueEmail();

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody(email, PASSWORD))
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody(
                                        email,
                                        "IncorrectPassword123!"
                                ))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message")
                        .value("Invalid email or password"));
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message")
                        .value("Authentication is required"));
    }

    @Test
    void shouldRejectTamperedJwt() throws Exception {
        String email = uniqueEmail();

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody(email, PASSWORD))
                )
                .andExpect(status().isCreated());

        String loginResponse = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody(email, PASSWORD))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = JsonPath.read(
                loginResponse,
                "$.accessToken"
        );

        String[] segments = token.split("\\.");

        assertEquals(3, segments.length);

        char firstCharacter = segments[2].charAt(0);
        char replacement = firstCharacter == 'A' ? 'B' : 'A';

        segments[2] = replacement + segments[2].substring(1);

        String tamperedToken = String.join(".", segments);

        mockMvc.perform(
                        get("/api/users/me")
                                .header(
                                        "Authorization",
                                        "Bearer " + tamperedToken
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void shouldRejectExpiredJwt() throws Exception {
        Instant issuedAt = Instant.now().minusSeconds(3600);
        Instant expiresAt = issuedAt.plusSeconds(900);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject("1")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("roles", List.of("USER"))
                .build();

        String expiredToken = jwtEncoder.encode(
                JwtEncoderParameters.from(claims)
        ).getTokenValue();

        mockMvc.perform(
                        get("/api/users/me")
                                .header(
                                        "Authorization",
                                        "Bearer " + expiredToken
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private String requestBody(String email, String password) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
    }
}