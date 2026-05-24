package com.neong.vixie.controllers.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neong.vixie.models.db.NotificationToken;
import com.neong.vixie.models.db.User;
import com.neong.vixie.models.dto.NotificationTokenRequest;
import com.neong.vixie.repositories.user.NotificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class NotificationTokenControllerTest {

    @Mock
    private NotificationTokenRepository notificationTokenRepository;

    @InjectMocks
    private NotificationTokenController notificationTokenController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User user;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationTokenController).build();
        objectMapper = new ObjectMapper();
        user = new User();
        user.setId("user_123");
        user.setEmail("user_email@example.com");
    }

    @Test
    void registerToken_createsNewTokenWhenNoneExists() throws Exception {
        NotificationTokenRequest request = new NotificationTokenRequest("device_abc", "fcm_xyz", "ANDROID");
        when(notificationTokenRepository.findByUserIdAndDeviceId(any(), eq("device_abc")))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/notifications/token")
                        .principal(new UsernamePasswordAuthenticationToken(user, null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        ArgumentCaptor<NotificationToken> captor = ArgumentCaptor.forClass(NotificationToken.class);
        verify(notificationTokenRepository).save(captor.capture());

        NotificationToken saved = captor.getValue();
        assertEquals("user_123", saved.getUserId());
        assertEquals("device_abc", saved.getDeviceId());
        assertEquals("fcm_xyz", saved.getFcmToken());
        assertEquals("ANDROID", saved.getPlatform());
        assertTrue(saved.isActive());
    }

    @Test
    void registerToken_updatesExistingTokenWhenOneExists() throws Exception {
        NotificationTokenRequest request = new NotificationTokenRequest("device_abc", "fcm_new", "IOS");
        
        NotificationToken existingToken = new NotificationToken();
        existingToken.setId("notif_token_1");
        existingToken.setUserId("user_123");
        existingToken.setDeviceId("device_abc");
        existingToken.setFcmToken("fcm_old");
        existingToken.setPlatform("ANDROID");
        existingToken.setActive(false);

        when(notificationTokenRepository.findByUserIdAndDeviceId(any(), eq("device_abc")))
                .thenReturn(Optional.of(existingToken));

        mockMvc.perform(post("/api/notifications/token")
                        .principal(new UsernamePasswordAuthenticationToken(user, null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        ArgumentCaptor<NotificationToken> captor = ArgumentCaptor.forClass(NotificationToken.class);
        verify(notificationTokenRepository).save(captor.capture());

        NotificationToken updated = captor.getValue();
        assertEquals("notif_token_1", updated.getId()); // Same ID
        assertEquals("user_123", updated.getUserId());
        assertEquals("device_abc", updated.getDeviceId());
        assertEquals("fcm_new", updated.getFcmToken());
        assertEquals("IOS", updated.getPlatform());
        assertTrue(updated.isActive());
    }
}
