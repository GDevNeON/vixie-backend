package com.neong.vixie.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neong.vixie.model.CharacterEntity;
import com.neong.vixie.model.RelationshipState;
import com.neong.vixie.repository.CharacterPersonalityRepository;
import com.neong.vixie.repository.CharacterRepository;
import com.neong.vixie.repository.RelationshipStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CharacterPromptServiceBehavioralTest {

    @Mock private CharacterRepository characterRepository;
    @Mock private CharacterPersonalityRepository characterPersonalityRepository;
    @Mock private RelationshipStateRepository relationshipStateRepository;
    @Mock private MoodService moodService;
    @Mock private UserInteractionProfileService profileService;

    private CharacterPromptService service;

    @BeforeEach
    void setUp() {
        service = new CharacterPromptService(
                characterRepository,
                characterPersonalityRepository,
                relationshipStateRepository,
                moodService,
                profileService,
                new ObjectMapper()
        );
    }

    @Test
    void buildSystemPrompt_isInCharacterWithPersonality() {
        CharacterEntity character = new CharacterEntity();
        character.setId("char_123");
        character.setName("Elena");
        character.setDescription("a stoic and formal AI companion");
        character.setDefaultSeriousness(0.9);
        character.setDefaultEnergy(0.2);
        character.setDefaultGentleness(0.4);

        when(characterRepository.findById("char_123")).thenReturn(Optional.of(character));
        when(characterPersonalityRepository.findByUserIdAndCharacterId(anyString(), anyString()))
                .thenReturn(Optional.empty());
        
        RelationshipState relState = new RelationshipState();
        relState.setLevel(8); // best friend
        when(relationshipStateRepository.findByUserIdAndCharacterId("user_1", "char_123"))
                .thenReturn(Optional.of(relState));
                
        when(moodService.getCurrentMood("user_1")).thenReturn("happy");
        when(profileService.getProfile("user_1")).thenReturn(null);

        String prompt = service.buildSystemPrompt("user_1", "char_123");

        assertTrue(prompt.contains("You are Elena, a stoic and formal AI companion"), "Must include character name and description");
        assertTrue(prompt.contains("Seriousness: 0.9/1.0"), "Must include character specific personality parameters");
        assertTrue(prompt.contains("Elena is currently feeling happy"), "Must include mood");
        assertTrue(prompt.contains("The user is a best friend"), "Must accurately translate relationship tier");
    }
}
