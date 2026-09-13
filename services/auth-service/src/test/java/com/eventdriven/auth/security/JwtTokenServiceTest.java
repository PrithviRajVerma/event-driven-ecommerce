package com.eventdriven.auth.security;

import com.eventdriven.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.UUID;

public class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;
    private final UUID userId = UUID.randomUUID();

    private final String email = "test@example.com";

    private final List<String> roles = List.of("USER");


    @BeforeEach
    void setup(){

        ;

        JwtProperties jwtProperties = new JwtProperties(
                "this-is-a-very-long-secret-key-for-testing-jwt-signing-123456",
                900_000,
                "auth-service"
        );
    }

    @Test
    void shoudGenerateAndValidateAccessToken(){

        String token = jwtTokenService.generateAccessToken(
                userId,
                email,
                roles
        );

        assertNotNull(token);

        Claims claim = jwtTokenService.parseAndValidate(token);

        assertEquals(
                userId.toString(),
                claim.getSubject()
        );

        assertEquals(
                email,
                claim.get("email",String.class)
        );

//        assertEquals(
//                roles,
//                claim.get("roles",List.class)
//        );

        assertNotNull(claim.getExpiration());
        assertNotNull(claim.getIssuedAt());


    }

}
