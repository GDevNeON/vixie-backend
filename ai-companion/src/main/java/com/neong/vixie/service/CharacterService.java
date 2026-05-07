package com.neong.vixie.service;

import com.neong.vixie.dto.CharacterResponse;
import com.neong.vixie.dto.CharacterStateResponse;
import com.neong.vixie.dto.PersonalityPatchRequest;
import com.neong.vixie.model.CharacterEntity;
import com.neong.vixie.model.CharacterPersonality;
import com.neong.vixie.model.RelationshipState;
import com.neong.vixie.repository.CharacterPersonalityRepository;
import com.neong.vixie.repository.CharacterRepository;
import com.neong.vixie.repository.RelationshipStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Business logic for character operations: listing, state retrieval, personality updates.
 */
@Service
@RequiredArgsConstructor
public class CharacterService {

    private final CharacterRepository characterRepository;
    private final CharacterPersonalityRepository characterPersonalityRepository;
    private final RelationshipStateRepository relationshipStateRepository;
    private final MoodService moodService;
    private final UserPreferencesService userPreferencesService;

    /**
     * Get all available characters.
     */
    public List<CharacterResponse> getAllCharacters() {
        return characterRepository.findAll().stream()
                .map(this::toCharacterResponse)
                .toList();
    }

    /**
     * Get a single character by ID.
     */
    public CharacterResponse getCharacter(String id) {
        CharacterEntity character = characterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + id));
        return toCharacterResponse(character);
    }

    /**
     * Get the user-specific state for a character (mood, relationship, personality settings).
     */
    public CharacterStateResponse getCharacterState(String userId, String characterId) {
        // Ensure character exists
        CharacterEntity character = characterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

        // Get mood from Redis
        String mood = moodService.getCurrentMood(userId);

        // Get relationship state (create default if not exists)
        RelationshipState relationship = relationshipStateRepository
                .findByUserIdAndCharacterId(userId, characterId)
                .orElseGet(() -> createDefaultRelationship(userId, characterId));

        // Get personality settings (fall back to character defaults)
        Map<String, Double> personalitySettings = characterPersonalityRepository
                .findByUserIdAndCharacterId(userId, characterId)
                .map(p -> Map.of(
                        "seriousness", p.getSeriousness(),
                        "energy", p.getEnergy(),
                        "gentleness", p.getGentleness()
                ))
                .orElse(Map.of(
                        "seriousness", character.getDefaultSeriousness(),
                        "energy", character.getDefaultEnergy(),
                        "gentleness", character.getDefaultGentleness()
                ));

        String activeCharacterId = userPreferencesService.getActiveCharacterId(userId);

        return new CharacterStateResponse(
                mood,
                relationship.getLevel(),
                relationship.getCurrentXp(),
                relationship.getXpToNextLevel(),
                personalitySettings,
                activeCharacterId
        );
    }

    /**
     * Update personality settings for a user-character pair.
     */
    public void updatePersonality(String userId, String characterId, PersonalityPatchRequest request) {
        // Ensure character exists
        characterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

        CharacterPersonality personality = characterPersonalityRepository
                .findByUserIdAndCharacterId(userId, characterId)
                .orElseGet(() -> CharacterPersonality.builder()
                        .id(UUID.randomUUID().toString())
                        .userId(userId)
                        .characterId(characterId)
                        .build());

        if (request.seriousness() != null) personality.setSeriousness(request.seriousness());
        if (request.energy() != null) personality.setEnergy(request.energy());
        if (request.gentleness() != null) personality.setGentleness(request.gentleness());

        characterPersonalityRepository.save(personality);
    }

    private CharacterResponse toCharacterResponse(CharacterEntity entity) {
        return new CharacterResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getAvatarUrl(),
                Map.of(
                        "seriousness", entity.getDefaultSeriousness(),
                        "energy", entity.getDefaultEnergy(),
                        "gentleness", entity.getDefaultGentleness()
                ),
                entity.getElevenlabsVoiceId()
        );
    }

    private RelationshipState createDefaultRelationship(String userId, String characterId) {
        RelationshipState state = RelationshipState.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .characterId(characterId)
                .level(1)
                .currentXp(0)
                .xpToNextLevel(100)
                .build();
        return relationshipStateRepository.save(state);
    }
}
