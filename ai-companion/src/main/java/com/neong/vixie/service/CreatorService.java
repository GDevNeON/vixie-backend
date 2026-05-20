package com.neong.vixie.service;

import com.neong.vixie.model.CreatorProfile;
import com.neong.vixie.repository.CreatorProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Service managing creator registration and profile operations.
 */
@Service
@RequiredArgsConstructor
public class CreatorService {

    private final CreatorProfileRepository creatorProfileRepository;

    /**
     * Register a user as a creator. Creates and persists a CreatorProfile.
     *
     * @param userId      the authenticated user's ID
     * @param displayName creator's display name
     * @param bio         optional bio text
     * @return the created CreatorProfile
     * @throws IllegalArgumentException if user is already a creator
     */
    public CreatorProfile registerCreator(String userId, String displayName, String bio) {
        if (creatorProfileRepository.existsByUserId(userId)) {
            throw new IllegalArgumentException("User is already registered as a creator");
        }

        CreatorProfile profile = CreatorProfile.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .displayName(displayName)
                .bio(bio)
                .termsAcceptedAt(Instant.now())
                .isVerified(false)
                .isBanned(false)
                .totalSales(0)
                .totalRevenue(0)
                .build();

        return creatorProfileRepository.save(profile);
    }

    /**
     * Get a creator profile by userId.
     */
    public Optional<CreatorProfile> getByUserId(String userId) {
        return creatorProfileRepository.findByUserId(userId);
    }

    /**
     * Check if a user is a registered creator.
     */
    public boolean isCreator(String userId) {
        return creatorProfileRepository.existsByUserId(userId);
    }
}
