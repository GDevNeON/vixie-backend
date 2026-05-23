package com.neong.vixie.config;
import com.neong.vixie.security.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for the AI Companion service.
 * - STATELESS session (JWT only, no cookies/sessions)
 * - Public: actuator, swagger/openapi, websocket handshake
 * - Protected: all /api/ai/** endpoints require valid JWT
 * - No login, no formLogin, no httpBasic — this service only validates tokens
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${api.internal.key:default-internal-key-123}")
    private String internalApiKey;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers(
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers("/ws/**").permitAll()  // WebSocket handshake
                        .requestMatchers("/api/marketplace/**").permitAll()  // Public catalog browsing
                        .requestMatchers("/api/gacha/banners", "/api/gacha/banners/**").permitAll()  // Public banner browsing
                        .requestMatchers("/api/internal/**").permitAll()  // Service-to-service (secured via X-Service-Key)
                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .addFilterBefore(new OncePerRequestFilter() {
                    @Override
                    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
                        if (request.getRequestURI().startsWith("/api/internal/")) {
                            String key = request.getHeader("X-Service-Key");
                            if (!internalApiKey.equals(key)) {
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                response.getWriter().write("Forbidden: Invalid X-Service-Key");
                                return;
                            }
                        }
                        filterChain.doFilter(request, response);
                    }
                }, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
