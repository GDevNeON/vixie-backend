package com.neong.vixie.controller;

import com.neong.vixie.dto.NotificationPreferencesRequest;
import com.neong.vixie.dto.NotificationPreferencesResponse;
import com.neong.vixie.model.NotificationPreferences;
import com.neong.vixie.repository.NotificationPreferencesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationPreferencesControllerTest {

    @Mock private NotificationPreferencesRepository preferencesRepository;
    @Mock private Principal principal;

    private NotificationPreferencesController controller;

    @BeforeEach
    void setUp() {
        controller = new NotificationPreferencesController(preferencesRepository);
        when(principal.getName()).thenReturn("user_123");
    }

    @Test
    void getPreferences_returnsDefaultsWithoutSavingWhenMissing() {
        when(preferencesRepository.findByUserIdAndCharacterId("user_123", "char_default"))
                .thenReturn(Optional.empty());

        ResponseEntity<NotificationPreferencesResponse> response =
                controller.getPreferences("char_default", principal);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("char_default", response.getBody().characterId());
        assertTrue(response.getBody().greetingEnabled());
        assertEquals(LocalTime.of(8, 0), response.getBody().greetingTime());
        assertEquals("Asia/Ho_Chi_Minh", response.getBody().timezone());
        verify(preferencesRepository, never()).save(any());
    }

    @Test
    void updatePreferences_createsOrUpdatesPreferenceRow() {
        when(preferencesRepository.findByUserIdAndCharacterId("user_123", "char_default"))
                .thenReturn(Optional.empty());
        when(preferencesRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationPreferencesRequest request = new NotificationPreferencesRequest(
                "char_default", false, LocalTime.of(9, 30),
                true, LocalTime.of(14, 0),
                true, LocalTime.of(22, 15), "Asia/Saigon"
        );

        ResponseEntity<NotificationPreferencesResponse> response =
                controller.updatePreferences(request, principal);

        ArgumentCaptor<NotificationPreferences> captor =
                ArgumentCaptor.forClass(NotificationPreferences.class);
        verify(preferencesRepository).save(captor.capture());

        NotificationPreferences saved = captor.getValue();
        assertEquals("user_123", saved.getUserId());
        assertEquals("char_default", saved.getCharacterId());
        assertFalse(saved.isGreetingEnabled());
        assertEquals(LocalTime.of(9, 30), saved.getGreetingTime());
        assertTrue(saved.isFocusEnabled());
        assertEquals(LocalTime.of(14, 0), saved.getFocusTime());
        assertTrue(saved.isSleepEnabled());
        assertEquals(LocalTime.of(22, 15), saved.getSleepTime());
        assertEquals("Asia/Saigon", saved.getTimezone());

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(LocalTime.of(9, 30), response.getBody().greetingTime());
    }
}
