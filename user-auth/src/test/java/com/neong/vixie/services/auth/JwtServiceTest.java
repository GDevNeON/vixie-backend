package com.neong.vixie.services.auth;

import com.neong.vixie.models.constant.AuthProvider;
import com.neong.vixie.models.constant.Role;
import com.neong.vixie.models.db.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMs", 60000L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpirationMs", 120000L);
    }

    @Test
    void generateAccessToken_includesStableUserIdClaim() {
        User user = User.builder()
                .id("user_123")
                .email("user@example.com")
                .password("unused")
                .role(Role.ROLE_USER)
                .authProvider(AuthProvider.LOCAL)
                .enabled(true)
                .locked(false)
                .build();

        String token = jwtService.generateAccessToken(user);

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertEquals("user@example.com", claims.getSubject());
        assertEquals("user_123", claims.get("user_id"));
    }
}
