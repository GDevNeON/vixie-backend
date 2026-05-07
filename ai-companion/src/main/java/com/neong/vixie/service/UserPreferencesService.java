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

    /**
     * Get the user's voice preferences. Returns defaults if no preferences record exists.
     */
    public UserPreferences getOrCreatePreferences(String userId) {
        return userPreferencesRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserPreferences prefs = UserPreferences.builder()
                            .id(UUID.randomUUID().toString())
                            .userId(userId)
                            .build();
                    return userPreferencesRepository.save(prefs);
                });
    }

    /**
     * Patch voice preferences. Only non-null fields are updated (partial update).
     */
    public void patchVoicePreferences(String userId, Boolean voiceMuted, Double voiceVolume) {
        UserPreferences preferences = getOrCreatePreferences(userId);

        if (voiceMuted != null) {
            preferences.setVoiceMuted(voiceMuted);
        }
        if (voiceVolume != null) {
            // Clamp volume to valid range
            preferences.setVoiceVolume(Math.max(0.0, Math.min(1.0, voiceVolume)));
        }

        userPreferencesRepository.save(preferences);
    }
}
