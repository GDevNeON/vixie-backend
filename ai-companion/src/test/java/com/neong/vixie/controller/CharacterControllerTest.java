package com.neong.vixie.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neong.vixie.dto.CharacterResponse;
import com.neong.vixie.dto.CharacterStateResponse;
import com.neong.vixie.dto.PersonalityPatchRequest;
import com.neong.vixie.service.CharacterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CharacterControllerTest {

    @Mock
    private CharacterService characterService;

    @InjectMocks
    private CharacterController characterController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(characterController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void getAllCharacters_ReturnsList() throws Exception {
        CharacterResponse response = new CharacterResponse("char_1", "Hana", "Desc", "url", Map.of("energy", 0.5), "voice_1");
        when(characterService.getAllCharacters()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/characters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("char_1"))
                .andExpect(jsonPath("$[0].name").value("Hana"));
    }

    @Test
    void getCharacter_ReturnsCharacter() throws Exception {
        CharacterResponse response = new CharacterResponse("char_1", "Hana", "Desc", "url", Map.of("energy", 0.5), "voice_1");
        when(characterService.getCharacter("char_1")).thenReturn(response);

        mockMvc.perform(get("/api/characters/char_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("char_1"))
                .andExpect(jsonPath("$.name").value("Hana"));
    }

    @Test
    void getCharacterState_ReturnsState() throws Exception {
        CharacterStateResponse response = new CharacterStateResponse("HAPPY", 2, 50, 200, Map.of("energy", 0.8), "char_1");
        when(characterService.getCharacterState(eq("user_1"), eq("char_1"))).thenReturn(response);

        mockMvc.perform(get("/api/characters/char_1/state").principal(() -> "user_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mood").value("HAPPY"))
                .andExpect(jsonPath("$.level").value(2))
                .andExpect(jsonPath("$.currentXp").value(50));
    }

    @Test
    void updatePersonality_ReturnsNoContent() throws Exception {
        PersonalityPatchRequest request = new PersonalityPatchRequest(0.9, null, null);
        
        mockMvc.perform(patch("/api/characters/char_1/personality")
                .principal(() -> "user_1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(characterService).updatePersonality(eq("user_1"), eq("char_1"), any(PersonalityPatchRequest.class));
    }
}
