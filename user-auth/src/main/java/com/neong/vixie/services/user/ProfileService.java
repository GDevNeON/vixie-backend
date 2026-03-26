package com.neong.vixie.services.user;

import com.neong.vixie.helpers.api.HtmlSanitizer;
import com.neong.vixie.helpers.api.IdGenerator;
import com.neong.vixie.models.db.User;
import com.neong.vixie.models.db.UserProfile;
import com.neong.vixie.models.dto.UpdateProfileRequest;
import com.neong.vixie.models.dto.UserProfileResponse;
import com.neong.vixie.repositories.user.UserProfileRepository;
import com.neong.vixie.repositories.user.UserRepository;
import com.neong.vixie.services.storage.StorageService;

import java.time.LocalDate;
import java.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);
    private static final int MINIMUM_AGE = 13;

    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    public ProfileService(UserProfileRepository userProfileRepository,
                          UserRepository userRepository,
                          StorageService storageService) {
        this.userProfileRepository = userProfileRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    /**
     * Get profile for authenticated user. Synthesizes a default if none exists.
     */
    public UserProfileResponse getProfile(String userId) {
        UserProfile profile = getOrCreateProfile(userId);
        return UserProfileResponse.fromEntity(profile);
    }

    /**
     * Partially update profile. Only non-null fields in the request are applied.
     */
    @Transactional
    public UserProfileResponse updateProfile(String userId, UpdateProfileRequest request) {
        UserProfile profile = getOrCreateProfile(userId);

        // Username uniqueness check
        if (request.username() != null && !request.username().equals(profile.getUsername())) {
            if (userProfileRepository.existsByUsername(request.username())) {
                throw new UsernameConflictException(request.username());
            }
            profile.setUsername(HtmlSanitizer.sanitize(request.username()));
        }

        // DOB immutability: if already set, reject changes
        if (request.dateOfBirth() != null) {
            if (profile.getDateOfBirth() != null
                    && !request.dateOfBirth().equals(profile.getDateOfBirth())) {
                throw new IllegalArgumentException("DOB_IMMUTABLE");
            }
            // Age validation (only applies when setting for the first time)
            int age = Period.between(request.dateOfBirth(), LocalDate.now()).getYears();
            if (age < MINIMUM_AGE) {
                throw new IllegalArgumentException("UNDERAGE");
            }
            profile.setDateOfBirth(request.dateOfBirth());
        }

        // Phone number structural validation
        if (request.phoneNumber() != null) {
            String phone = request.phoneNumber().trim();
            if (!phone.startsWith("+") || phone.length() < 8 || phone.length() > 16) {
                throw new IllegalArgumentException("INVALID_PHONE_FORMAT");
            }
            profile.setPhoneNumber(phone);
        }

        // Partial merge with sanitization for string fields
        if (request.displayName() != null) {
            profile.setDisplayName(HtmlSanitizer.sanitize(request.displayName()));
        }
        if (request.bio() != null) {
            profile.setBio(HtmlSanitizer.sanitize(request.bio()));
        }
        if (request.gender() != null) {
            profile.setGender(request.gender());
        }
        if (request.country() != null) {
            profile.setCountry(request.country());
        }
        if (request.location() != null) {
            profile.setLocation(HtmlSanitizer.sanitize(request.location()));
        }

        UserProfile saved = userProfileRepository.save(profile);
        log.info("Profile updated for user_id={}, changed_fields=[{}]",
                userId, buildChangedFieldsLog(request));
        return UserProfileResponse.fromEntity(saved);
    }

    /**
     * Upload avatar file to cloud storage and update the profile.
     */
    @Transactional
    public UserProfileResponse uploadAvatar(String userId, MultipartFile file) {
        UserProfile profile = getOrCreateProfile(userId);
        String avatarUrl = storageService.uploadAvatar(file, userId);
        profile.setAvatarUrl(avatarUrl);
        UserProfile saved = userProfileRepository.save(profile);
        log.info("Avatar uploaded for user_id={}", userId);
        return UserProfileResponse.fromEntity(saved);
    }

    /**
     * Update avatar URL.
     */
    @Transactional
    public UserProfileResponse updateAvatar(String userId, String avatarUrl) {
        UserProfile profile = getOrCreateProfile(userId);
        profile.setAvatarUrl(HtmlSanitizer.sanitize(avatarUrl));
        UserProfile saved = userProfileRepository.save(profile);
        log.info("Avatar updated for user_id={}", userId);
        return UserProfileResponse.fromEntity(saved);
    }

    /**
     * Remove avatar (set to null).
     */
    @Transactional
    public void removeAvatar(String userId) {
        userProfileRepository.findByUserId(userId).ifPresent(profile -> {
            profile.setAvatarUrl(null);
            userProfileRepository.save(profile);
            log.info("Avatar removed for user_id={}", userId);
        });
    }

    // --- Private helpers ---

    private UserProfile getOrCreateProfile(String userId) {
        return userProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found"));
                    String emailPrefix = user.getEmail().split("@")[0];

                    UserProfile newProfile = UserProfile.builder()
                            .id(IdGenerator.generateId("user_profiles"))
                            .user(user)
                            .username(emailPrefix)
                            .displayName(emailPrefix)
                            .build();
                    log.info("Synthesized default profile for user_id={}", userId);
                    return userProfileRepository.save(newProfile);
                });
    }

    private String buildChangedFieldsLog(UpdateProfileRequest request) {
        StringBuilder sb = new StringBuilder();
        if (request.username() != null) sb.append("username,");
        if (request.displayName() != null) sb.append("display_name,");
        if (request.bio() != null) sb.append("bio,");
        if (request.gender() != null) sb.append("gender,");
        if (request.country() != null) sb.append("country,");
        if (request.location() != null) sb.append("location,");
        // Intentionally NOT logging phone_number or date_of_birth (PII)
        if (sb.length() > 0) sb.setLength(sb.length() - 1); // remove trailing comma
        return sb.toString();
    }
}
