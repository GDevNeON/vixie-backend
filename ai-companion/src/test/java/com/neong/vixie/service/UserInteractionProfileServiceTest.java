package com.neong.vixie.service;

import com.neong.vixie.model.UserInteractionProfile;
import com.neong.vixie.repository.UserInteractionProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserInteractionProfileServiceTest {

    @Mock
    private UserInteractionProfileRepository profileRepository;

    @InjectMocks
    private UserInteractionProfileService profileService;

    @Captor
    private ArgumentCaptor<UserInteractionProfile> profileCaptor;

    @Test
    void onSessionDisconnect_UpdatesProfile() {
        SessionDisconnectEvent event = mock(SessionDisconnectEvent.class);
        Principal principal = mock(Principal.class);
        when(event.getUser()).thenReturn(principal);
        when(principal.getName()).thenReturn("user_1");

        UserInteractionProfile profile = new UserInteractionProfile();
        profile.setId("prof_1");
        profile.setUserId("user_1");
        profile.setTotalSessionCount(5);
        profile.setPreferredActiveHours("[]");

        when(profileRepository.findByUserId("user_1")).thenReturn(Optional.of(profile));

        profileService.onSessionDisconnect(event);

        verify(profileRepository).save(profileCaptor.capture());
        UserInteractionProfile savedProfile = profileCaptor.getValue();
        assertEquals(6, savedProfile.getTotalSessionCount());
        assertNotNull(savedProfile.getLastSessionAt());
    }

    @Test
    void onSessionDisconnect_NullPrincipal_DoesNothing() {
        SessionDisconnectEvent event = mock(SessionDisconnectEvent.class);
        when(event.getUser()).thenReturn(null);

        profileService.onSessionDisconnect(event);

        verifyNoInteractions(profileRepository);
    }

    @Test
    void updateConversationMetrics_UpdatesAveragesAndTopics() {
        UserInteractionProfile profile = new UserInteractionProfile();
        profile.setId("prof_1");
        profile.setUserId("user_1");
        profile.setTotalMessageCount(2);
        profile.setAvgMessageLength(10.0);
        profile.setTopTopics("[\"food\"]");

        when(profileRepository.findByUserId("user_1")).thenReturn(Optional.of(profile));

        profileService.updateConversationMetrics("user_1", 20.0, List.of("music", "food"));

        verify(profileRepository).save(profileCaptor.capture());
        UserInteractionProfile savedProfile = profileCaptor.getValue();

        // New average: (10.0 * 2 + 20.0) / 3 = 40.0 / 3 = 13.333
        assertEquals(13.333, savedProfile.getAvgMessageLength(), 0.01);
        assertEquals(3, savedProfile.getTotalMessageCount());
        assertTrue(savedProfile.getTopTopics().contains("music"));
        assertTrue(savedProfile.getTopTopics().contains("food"));
        assertEquals("PLAYFUL", savedProfile.getPreferredTone());
    }
}
