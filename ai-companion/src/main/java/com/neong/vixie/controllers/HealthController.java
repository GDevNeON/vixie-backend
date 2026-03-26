package com.neong.vixie.controllers;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health check endpoint for the AI Companion service.
 * Protected by JWT — proves that shared-secret validation works.
 */
@RestController
@RequestMapping("/api/ai")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        String principal = SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal()
                .toString();

        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "service", "ai-companion",
                "user", principal
        ));
    }
}
