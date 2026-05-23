package com.neong.vixie.controller;

import com.neong.vixie.dto.ActiveCharacterRequest;
import com.neong.vixie.dto.VoicePreferencesPatchRequest;
import com.neong.vixie.dto.VoicePreferencesResponse;
import com.neong.vixie.model.UserPreferences;
import com.neong.vixie.service.UserPreferencesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

/**
 * REST controller for user preferences.
 * - POST /api/users/preferences/active-character — set active companion
 * - GET /api/users/preferences/voice — get voice preferences
 * - PATCH /api/users/preferences/voice — update voice preferences (partial)
 */
@RestController
@RequestMapping("/api/users/preferences")
@RequiredArgsConstructor
public class UserPreferencesController {

    private final UserPreferencesService userPreferencesService;

    @PostMapping("/active-character")
    public ResponseEntity<Void> setActiveCharacter(
            @RequestBody ActiveCharacterRequest request,
            Principal principal) {
        String userId = principal.getName();
        userPreferencesService.setActiveCharacter(userId, request.characterId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/active-character")
    public ResponseEntity<Map<String, String>> getActiveCharacter(Principal principal) {
        String userId = principal.getName();
        String characterId = userPreferencesService.getActiveCharacterId(userId);
        if (characterId == null) {
            return ResponseEntity.ok(Map.of());
        }
        return ResponseEntity.ok(Map.of("character_id", characterId));
    }

    @GetMapping("/voice")
    public ResponseEntity<VoicePreferencesResponse> getVoicePreferences(Principal principal) {
        String userId = principal.getName();
        UserPreferences prefs = userPreferencesService.getOrCreatePreferences(userId);
        return ResponseEntity.ok(new VoicePreferencesResponse(
                prefs.getVoiceMuted(),
                prefs.getVoiceVolume()
        ));
    }

    @PatchMapping("/voice")
    public ResponseEntity<Void> patchVoicePreferences(
            @RequestBody VoicePreferencesPatchRequest request,
            Principal principal) {
        String userId = principal.getName();
        userPreferencesService.patchVoicePreferences(userId, request.voiceMuted(), request.voiceVolume());
        return ResponseEntity.noContent().build();
    }
}

