package com.neong.vixie.dto;

import com.neong.vixie.model.CreatorProfile;

import java.time.Instant;

/**
 * Response DTO for creator profile data.
 */
public record CreatorProfileDto(
        String id,
        String userId,
        String displayName,
        String bio,
        Instant termsAcceptedAt,
        boolean isVerified,
        int totalSales,
        int totalRevenue
) {
    public static CreatorProfileDto from(CreatorProfile profile) {
        return new CreatorProfileDto(
                profile.getId(),
                profile.getUserId(),
                profile.getDisplayName(),
                profile.getBio(),
                profile.getTermsAcceptedAt(),
                Boolean.TRUE.equals(profile.getIsVerified()),
                profile.getTotalSales() != null ? profile.getTotalSales() : 0,
                profile.getTotalRevenue() != null ? profile.getTotalRevenue() : 0
        );
    }
}
