package com.neong.vixie.controller;

import com.neong.vixie.dto.ActiveCharacterRequest;
import com.neong.vixie.service.UserPreferencesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * REST controller for user preferences.
 * - POST /api/users/preferences/active-character — set active companion
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
}
