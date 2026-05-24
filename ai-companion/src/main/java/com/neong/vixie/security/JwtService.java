package com.neong.vixie.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * JWT validation service for the AI Companion.
 * This service ONLY validates tokens issued by user-auth — it does NOT issue tokens.
 * It shares the same HMAC-SHA256 signing key as user-auth.
 */
@Slf4j
@Service
public class JwtService {

    @Value("${security.jwt.secret}")
    private String secret;

    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Extract the subject (email) from the token.
     * In user-auth, the subject is set to userDetails.getUsername() which is the email.
     */
    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract the stable user id if present. Older tokens may not have this claim,
     * so callers can fall back to the subject for backwards compatibility.
     */
    public String extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        Object userId = claims.get("user_id");
        return userId != null ? userId.toString() : claims.getSubject();
    }

    /**
     * Extract the "type" claim to distinguish access vs refresh tokens.
     */
    public String extractTokenType(String token) {
        Claims claims = extractAllClaims(token);
        Object type = claims.get("type");
        return type != null ? type.toString() : null;
    }

    /**
     * Validate a token: checks signature, expiration, and ensures it's an access token.
     * Returns true if valid, false otherwise.
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            boolean isExpired = claims.getExpiration().before(new Date());
            if (isExpired) {
                log.debug("Token has expired");
                return false;
            }
            // Only accept access tokens, not refresh tokens
            Object type = claims.get("type");
            if (type != null && "refresh".equals(type.toString())) {
                log.debug("Rejected refresh token — only access tokens are accepted");
                return false;
            }
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
