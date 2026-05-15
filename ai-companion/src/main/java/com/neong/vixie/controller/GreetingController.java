package com.neong.vixie.controller;

import com.neong.vixie.service.GreetingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for daily greeting system.
 *
 * Phase 8: Provides the endpoint Flutter calls when CharacterScreen opens
 * to fetch or generate the user's daily personalized greeting.
 */
@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
@Slf4j
public class GreetingController {

    private final GreetingService greetingService;

    /**
     * Get the daily greeting for the authenticated user and character.
     *
     * Returns:
     * - {"greeted": false, "message": "..."} on first call of the day
     * - {"greeted": true, "message": "..."} if already greeted (cached)
     * - {"greeted": true} if greeted but cache expired
     */
    @GetMapping("/{characterId}/greeting/daily")
    public ResponseEntity<Map<String, Object>> getDailyGreeting(
            @PathVariable String characterId,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        Map<String, Object> result = greetingService.getDailyGreeting(
                user.getUsername(), characterId);

        return ResponseEntity.ok(result);
    }
}
