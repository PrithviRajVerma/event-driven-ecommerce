package com.eventdriven.product.security;

import com.eventdriven.product.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
public class JwtTokenService {

    private final SecretKey signingKey;
    private final String issuer;

    public JwtTokenService(JwtProperties jwtProperties) {
        this.signingKey = Keys.hmacShaKeyFor(
                jwtProperties.secret().getBytes(StandardCharsets.UTF_8)
        );
        this.issuer = jwtProperties.issuer();
    }

    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID getUserId(String token) {
        return UUID.fromString(
                parseAndValidate(token).getSubject()
        );
    }

    public List<String> getRoles(String token) {
        return parseAndValidate(token)
                .get("roles", List.class);
    }
}