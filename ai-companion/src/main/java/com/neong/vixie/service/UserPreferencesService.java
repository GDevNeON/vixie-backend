package com.neong.vixie.service;

import com.neong.vixie.model.UserPreferences;
import com.neong.vixie.repository.CharacterRepository;
import com.neong.vixie.repository.UserPreferencesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for managing user preferences (active companion selection).
 */
@Service
@RequiredArgsConstructor
public class UserPreferencesService {

    private final UserPreferencesRepository userPreferencesRepository;
    private final CharacterRepository characterRepository;

    /**
     * Set the user's active character. Validates character exists first.
     */
    public void setActiveCharacter(String userId, String characterId) {
        // Validate character exists
        characterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

        UserPreferences preferences = userPreferencesRepository.findByUserId(userId)
                .orElseGet(() -> UserPreferences.builder()
                        .id(UUID.randomUUID().toString())
                        .userId(userId)
                        .build());

        preferences.setActiveCharacterId(characterId);
        userPreferencesRepository.save(preferences);
    }

    /**
     * Get the user's active character ID. Returns null if no selection made.
     */
    public String getActiveCharacterId(String userId) {
        return userPreferencesRepository.findByUserId(userId)
                .map(UserPreferences::getActiveCharacterId)
                .orElse(null);
    }
}
