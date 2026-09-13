package com.eventdriven.auth.security;

import com.eventdriven.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtTokenService {

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtTokenService(JwtProperties jwtProperties){
        this.jwtProperties = jwtProperties;

        this.signingKey = Keys.hmacShaKeyFor(
                jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(
            UUID userId,
            String email,
            List<String> roles
    ){
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email",email)
                .claim("roles",roles)
                .issuer(jwtProperties.issuer())
                .issuedAt(Date.from(now))
                .expiration(
                        Date.from(
                                now.plusMillis(jwtProperties.accessTokenExpiration())
                        )
                )
                .signWith(signingKey)
                .compact();
    }

    public Claims parseAndValidate(String token){

        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(jwtProperties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();

    }

    public UUID getUserId(String token){
        Claims claim = parseAndValidate(token);

        return UUID.fromString(claim.getSubject());
    }

    public List<String> getRoles(String token){
        Claims claim = parseAndValidate(token);

        return claim.get("roles",List.class);
    }

    public String getEmail(String token){
        Claims claim = parseAndValidate(token);

        return claim.get("email",String.class);
    }

}
