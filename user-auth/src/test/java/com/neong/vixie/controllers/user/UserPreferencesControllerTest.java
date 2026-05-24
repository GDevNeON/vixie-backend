package com.neong.vixie.controllers.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neong.vixie.models.db.User;
import com.neong.vixie.models.db.UserPreferences;
import com.neong.vixie.models.dto.VoicePreferencesDto;
import com.neong.vixie.repositories.user.UserPreferencesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class UserPreferencesControllerTest {

    @Mock
    private UserPreferencesRepository userPreferencesRepository;

    @InjectMocks
    private UserPreferencesController userPreferencesController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User user;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userPreferencesController).build();
        objectMapper = new ObjectMapper();
        user = new User();
        user.setId("user_123");
    }

    @Test
    void getVoicePreferences_returnsExistingPreferences() throws Exception {
        UserPreferences prefs = new UserPreferences();
        prefs.setVoiceMuted(true);
        prefs.setVoiceVolume(0.5);
        when(userPreferencesRepository.findByUserId(any())).thenReturn(Optional.of(prefs));

        mockMvc.perform(get("/api/users/preferences/voice")
                        .principal(new UsernamePasswordAuthenticationToken(user, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voice_muted").value(true))
                .andExpect(jsonPath("$.voice_volume").value(0.5));
    }

    @Test
    void getVoicePreferences_returnsDefaultsWhenNoPreferences() throws Exception {
        when(userPreferencesRepository.findByUserId(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/preferences/voice")
                        .principal(new UsernamePasswordAuthenticationToken(user, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voice_muted").value(false))
                .andExpect(jsonPath("$.voice_volume").value(1.0));
    }

    @Test
    void updateVoicePreferences_updatesAndReturnsNewPreferences() throws Exception {
        UserPreferences prefs = new UserPreferences();
        prefs.setId("preference_123");
        prefs.setVoiceMuted(false);
        prefs.setVoiceVolume(1.0);
        when(userPreferencesRepository.findByUserId(any())).thenReturn(Optional.of(prefs));
        
        VoicePreferencesDto request = new VoicePreferencesDto(true, 0.7);

        mockMvc.perform(patch("/api/users/preferences/voice")
                        .principal(new UsernamePasswordAuthenticationToken(user, null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voice_muted").value(true))
                .andExpect(jsonPath("$.voice_volume").value(0.7));

        verify(userPreferencesRepository).save(prefs);
    }
}
