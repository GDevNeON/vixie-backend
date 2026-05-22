package com.neong.vixie.controller;

import com.neong.vixie.dto.NotificationPreferencesRequest;
import com.neong.vixie.dto.NotificationPreferencesResponse;
import com.neong.vixie.model.NotificationPreferences;
import com.neong.vixie.repository.NotificationPreferencesRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/notifications/preferences")
@RequiredArgsConstructor
public class NotificationPreferencesController {

    private final NotificationPreferencesRepository preferencesRepository;

    @GetMapping
    public ResponseEntity<NotificationPreferencesResponse> getPreferences(
            @RequestParam String characterId,
            Principal principal) {
        String userId = principal.getName();

        NotificationPreferences preferences = preferencesRepository
                .findByUserIdAndCharacterId(userId, characterId)
                .orElseGet(() -> NotificationPreferences.defaults(userId, characterId));

        return ResponseEntity.ok(NotificationPreferencesResponse.from(preferences));
    }

    @PutMapping
    public ResponseEntity<NotificationPreferencesResponse> updatePreferences(
            @Valid @RequestBody NotificationPreferencesRequest request,
            Principal principal) {
        String userId = principal.getName();
        validateTimezone(request.timezone());

        NotificationPreferences preferences = preferencesRepository
                .findByUserIdAndCharacterId(userId, request.characterId())
                .orElseGet(() -> NotificationPreferences.defaults(userId, request.characterId()));

        if (request.greetingEnabled() != null) {
            preferences.setGreetingEnabled(request.greetingEnabled());
        }
        if (request.greetingTime() != null) {
            preferences.setGreetingTime(request.greetingTime());
        }
        if (request.focusEnabled() != null) {
            preferences.setFocusEnabled(request.focusEnabled());
        }
        preferences.setFocusTime(request.focusTime());
        if (request.sleepEnabled() != null) {
            preferences.setSleepEnabled(request.sleepEnabled());
        }
        preferences.setSleepTime(request.sleepTime());
        if (request.timezone() != null) {
            preferences.setTimezone(request.timezone());
        }
        preferences.applyDefaults();

        NotificationPreferences saved = preferencesRepository.save(preferences);
        return ResponseEntity.ok(NotificationPreferencesResponse.from(saved));
    }

    private void validateTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return;
        }
        ZoneId.of(timezone);
    }
}
