package com.iftikar.outlier.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {
    private final Long expireationTime;
    private final Long refreshExpirationTime;
    private final String secret;

    public JwtService(@Value("${jwt.secret}") String secret, @Value("${jwt.access-expiration}") Long expireationTime, @Value("${jwt.refresh-expiration}") Long refreshExpirationTime) {
        this.expireationTime = expireationTime;
        this.refreshExpirationTime = refreshExpirationTime;
        this.secret = secret;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateToken(String userId, String username, String role, boolean isAccessToken) {
        Long expTime = isAccessToken ? expireationTime : refreshExpirationTime;
        String tokenType = isAccessToken ? "access" : "refresh";
        Map<String, Object> claims = Map.of(
                "type", tokenType,
                "username", username,
                "isDeveloper", role
        );

        return Jwts.builder()
                .subject(userId)
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expTime))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Parses and verifies the JWT.
     *
     * This method will throw JwtException if:
     * - the token is malformed
     * - the signature is invalid
     * - the token is expired
     * - the token cannot be parsed
     */

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isAccessToken(Claims claims) {
        String type = claims.get("type", String.class);
        return "access".equals(type);
    }

    public boolean isRefreshToken(Claims claims) {
        String type = claims.get("type", String.class);
        return "refresh".equals(type);
    }
}
