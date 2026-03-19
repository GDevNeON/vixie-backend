package com.neong.vixie.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.neong.vixie.controllers.user.ProfileController;
import com.neong.vixie.helpers.api.GlobalExceptionHandler;
import com.neong.vixie.models.dto.UpdateAvatarRequest;
import com.neong.vixie.models.dto.UpdateProfileRequest;
import com.neong.vixie.models.dto.UserProfileResponse;
import com.neong.vixie.services.user.ProfileService;
import com.neong.vixie.services.user.UsernameConflictException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private ProfileController profileController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(profileController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(converter)
                .build();
    }

    private UserProfileResponse sampleResponse() {
        return new UserProfileResponse(
                "profile_123", "user_123", "testuser", "Test User",
                "Bio text", null, null, null, null, "VN", null, null, null
        );
    }

    @Test
    void getProfile_returns200() throws Exception {
        when(profileService.getProfile(any())).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/users/me/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.display_name").value("Test User"));
    }

    @Test
    void updateProfile_validBody_returns200() throws Exception {
        when(profileService.updateProfile(any(), any())).thenReturn(sampleResponse());

        UpdateProfileRequest request = new UpdateProfileRequest(
                "newuser", "New Name", null, null, null, null, null, null
        );

        mockMvc.perform(put("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void updateProfile_usernameConflict_returns409() throws Exception {
        when(profileService.updateProfile(any(), any()))
                .thenThrow(new UsernameConflictException("taken_name"));

        UpdateProfileRequest request = new UpdateProfileRequest(
                "taken_name", null, null, null, null, null, null, null
        );

        mockMvc.perform(put("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("USERNAME_TAKEN"));
    }

    @Test
    void updateAvatar_validUrl_returns200() throws Exception {
        when(profileService.updateAvatar(any(), any())).thenReturn(sampleResponse());

        UpdateAvatarRequest request = new UpdateAvatarRequest("https://example.com/avatar.png");

        mockMvc.perform(put("/api/users/me/avatar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void removeAvatar_returns204() throws Exception {
        doNothing().when(profileService).removeAvatar(any());

        mockMvc.perform(delete("/api/users/me/avatar"))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateProfile_underage_returns400() throws Exception {
        when(profileService.updateProfile(any(), any()))
                .thenThrow(new IllegalArgumentException("UNDERAGE"));

        UpdateProfileRequest request = new UpdateProfileRequest(
                null, null, null, null, null, null, null, null
        );

        mockMvc.perform(put("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("UNDERAGE"));
    }
}
