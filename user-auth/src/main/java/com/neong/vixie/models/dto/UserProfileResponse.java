package com.neong.vixie.models.dto;

import com.neong.vixie.models.db.UserProfile;

public record UserProfileResponse(
        String profileId,
        String userId,
        String username,
        String displayName,
        String bio,
        String avatarUrl,
        String phoneNumber,
        String gender,
        String dateOfBirth,
        String country,
        String location,
        String createdAt,
        String updatedAt
) {
    public static UserProfileResponse fromEntity(UserProfile profile) {
        return new UserProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getUsername(),
                profile.getDisplayName(),
                profile.getBio(),
                profile.getAvatarUrl(),
                profile.getPhoneNumber(),
                profile.getGender() != null ? profile.getGender().name() : null,
                profile.getDateOfBirth() != null ? profile.getDateOfBirth().toString() : null,
                profile.getCountry(),
                profile.getLocation(),
                profile.getCreatedAt() != null ? profile.getCreatedAt().toString() : null,
                profile.getUpdatedAt() != null ? profile.getUpdatedAt().toString() : null
        );
    }
}
