package com.benhsoan.infrastructure.authSecurity;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenAdapter implements JwtTokenPort {

    private final Key secretKey;

    private final long expiration;

    public JwtTokenAdapter(
        @Value("${app.jwt.secret}") String secret,
        @Value("${app.jwt.expiration-ms}") long expiration
) {
    this.secretKey = Keys.hmacShaKeyFor(
            secret.getBytes(StandardCharsets.UTF_8)
    );

    this.expiration = expiration;
}

    @Override
    public String generateToken(
            UUID userId,
            UUID sessionId,
            String username,
            String role
    ) {

        Date now = new Date();

        Date expired = new Date(
                now.getTime() + expiration
        );

        return Jwts.builder()
                .subject(userId.toString())
                .claim("sessionId", sessionId.toString())
                .claim("userId", userId.toString())
                .claim("username", username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expired)
                .signWith(secretKey)
                .compact();
    }

    @Override
    public UUID getUserId(String token) {
        return UUID.fromString(
                getClaims(token).getSubject()
        );
    }

    @Override
    public String getUsername(String token) {
        return getClaims(token)
                .get("username", String.class);
    }

    @Override
    public String getRole(String token) {
        return getClaims(token)
                .get("role", String.class);
    }

    @Override
    public UUID getSessionId(String token) {
        return UUID.fromString(getClaims(token).get("sessionId", String.class));
    }

    @Override
    public boolean validate(String token) {

        try {

            getClaims(token);

            return true;

        } catch (Exception ex) {

            return false;
        }
    }

    @Override
        public Instant getExpiredAt(String token) {
            return getClaims(token)
            .getExpiration()
            .toInstant();
        }

        @Override
        public boolean isExpired(String token) {
            return getExpiredAt(token)
            .isBefore(Instant.now());
        }


    private Claims getClaims(String token) {

        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
}
