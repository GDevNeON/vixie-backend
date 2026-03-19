package com.neong.vixie.services;

import com.neong.vixie.helpers.api.IdGenerator;
import com.neong.vixie.models.constant.AuthProvider;
import com.neong.vixie.models.constant.Gender;
import com.neong.vixie.models.constant.Role;
import com.neong.vixie.models.db.User;
import com.neong.vixie.models.db.UserProfile;
import com.neong.vixie.models.dto.UpdateProfileRequest;
import com.neong.vixie.models.dto.UserProfileResponse;
import com.neong.vixie.repositories.user.UserProfileRepository;
import com.neong.vixie.repositories.user.UserRepository;
import com.neong.vixie.services.user.ProfileService;
import com.neong.vixie.services.user.UsernameConflictException;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProfileService profileService;

    private User testUser;
    private UserProfile testProfile;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user_123")
                .email("test@example.com")
                .role(Role.ROLE_USER)
                .authProvider(AuthProvider.LOCAL)
                .enabled(true)
                .locked(false)
                .build();

        testProfile = UserProfile.builder()
                .id("profile_123")
                .user(testUser)
                .username("testuser")
                .displayName("Test User")
                .bio("Hello world")
                .country("VN")
                .build();
    }

    @Test
    void getProfile_existingProfile_returnsResponse() {
        when(userProfileRepository.findByUserId("user_123"))
                .thenReturn(Optional.of(testProfile));

        UserProfileResponse response = profileService.getProfile("user_123");

        assertEquals("profile_123", response.profileId());
        assertEquals("testuser", response.username());
        assertEquals("Test User", response.displayName());
        assertEquals("VN", response.country());
    }

    @Test
    void getProfile_noProfile_synthesizesDefault() {
        when(userProfileRepository.findByUserId("user_123"))
                .thenReturn(Optional.empty());
        when(userRepository.findById("user_123"))
                .thenReturn(Optional.of(testUser));
        when(userProfileRepository.save(any(UserProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = profileService.getProfile("user_123");

        assertNotNull(response);
        assertEquals("test", response.username()); // email prefix
        assertEquals("test", response.displayName()); // email prefix
        verify(userProfileRepository).save(any(UserProfile.class));
    }

    @Test
    void updateProfile_happyPath_updatesFields() {
        when(userProfileRepository.findByUserId("user_123"))
                .thenReturn(Optional.of(testProfile));
        when(userProfileRepository.save(any(UserProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest(
                null, "New Name", "New bio", null, null, null, null, "Ho Chi Minh"
        );

        UserProfileResponse response = profileService.updateProfile("user_123", request);

        assertEquals("New Name", response.displayName());
        assertEquals("New bio", response.bio());
        assertEquals("Ho Chi Minh", response.location());
        assertEquals("testuser", response.username()); // unchanged
    }

    @Test
    void updateProfile_usernameConflict_throwsException() {
        when(userProfileRepository.findByUserId("user_123"))
                .thenReturn(Optional.of(testProfile));
        when(userProfileRepository.existsByUsername("taken_name"))
                .thenReturn(true);

        UpdateProfileRequest request = new UpdateProfileRequest(
                "taken_name", null, null, null, null, null, null, null
        );

        assertThrows(UsernameConflictException.class,
                () -> profileService.updateProfile("user_123", request));
    }

    @Test
    void updateProfile_underage_throwsException() {
        when(userProfileRepository.findByUserId("user_123"))
                .thenReturn(Optional.of(testProfile));

        LocalDate underageDob = LocalDate.now().minusYears(10);
        UpdateProfileRequest request = new UpdateProfileRequest(
                null, null, null, null, null, underageDob, null, null
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> profileService.updateProfile("user_123", request));
        assertEquals("UNDERAGE", ex.getMessage());
    }

    @Test
    void updateAvatar_setsUrl() {
        when(userProfileRepository.findByUserId("user_123"))
                .thenReturn(Optional.of(testProfile));
        when(userProfileRepository.save(any(UserProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = profileService.updateAvatar(
                "user_123", "https://example.com/avatar.png");

        assertEquals("https://example.com/avatar.png", response.avatarUrl());
    }

    @Test
    void removeAvatar_clearsUrl() {
        when(userProfileRepository.findByUserId("user_123"))
                .thenReturn(Optional.of(testProfile));

        profileService.removeAvatar("user_123");

        assertNull(testProfile.getAvatarUrl());
        verify(userProfileRepository).save(testProfile);
    }
}
