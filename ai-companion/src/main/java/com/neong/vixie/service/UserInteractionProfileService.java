package com.neong.vixie.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neong.vixie.model.UserInteractionProfile;
import com.neong.vixie.repository.UserInteractionProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Learns user interaction preferences from conversation sessions.
 * Updates the user's profile asynchronously on WebSocket session disconnect.
 *
 * Phase 8: Feeds into CharacterPromptService personalization clause.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserInteractionProfileService {

    private final UserInteractionProfileRepository profileRepository;
    private final ObjectMapper objectMapper;

    private static final int MAX_TOPICS = 10;

    /**
     * Update user's interaction profile when a STOMP session disconnects.
     * Runs asynchronously to avoid blocking the session teardown.
     */
    @Async
    @EventListener
    public void onSessionDisconnect(SessionDisconnectEvent event) {
        try {
            var principal = event.getUser();
            if (principal == null) {
                log.debug("Session disconnect with no principal — skipping profile update");
                return;
            }
            String userId = principal.getName();
            updateProfileOnDisconnect(userId);
        } catch (Exception e) {
            log.warn("Failed to update interaction profile on disconnect: {}", e.getMessage());
        }
    }

    /**
     * Update profile stats on session disconnect.
     * Increments session count, updates active hours, and last session timestamp.
     */
    public void updateProfileOnDisconnect(String userId) {
        UserInteractionProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserInteractionProfile newProfile = UserInteractionProfile.builder()
                            .id(UUID.randomUUID().toString())
                            .userId(userId)
                            .build();
                    return profileRepository.save(newProfile);
                });

        // Increment session count
        profile.setTotalSessionCount(profile.getTotalSessionCount() + 1);
        profile.setLastSessionAt(Instant.now());

        // Update preferred active hours
        updateActiveHours(profile);

        profileRepository.save(profile);
        log.info("Updated interaction profile for user={} (sessions={})",
                userId, profile.getTotalSessionCount());
    }

    /**
     * Update the user's message length average and topic list.
     * Called from SummarizationService after extracting topics.
     */
    public void updateConversationMetrics(String userId, double avgLength, List<String> topics) {
        UserInteractionProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserInteractionProfile newProfile = UserInteractionProfile.builder()
                            .id(UUID.randomUUID().toString())
                            .userId(userId)
                            .build();
                    return profileRepository.save(newProfile);
                });

        // Rolling average of message length
        int count = profile.getTotalMessageCount();
        double currentAvg = profile.getAvgMessageLength();
        double newAvg = count > 0
                ? (currentAvg * count + avgLength) / (count + 1)
                : avgLength;
        profile.setAvgMessageLength(newAvg);
        profile.setTotalMessageCount(count + 1);

        // Merge topics (keep top N unique)
        List<String> existingTopics = parseJsonArray(profile.getTopTopics());
        for (String topic : topics) {
            if (!existingTopics.contains(topic)) {
                existingTopics.add(topic);
            }
        }
        // Keep only latest MAX_TOPICS
        if (existingTopics.size() > MAX_TOPICS) {
            existingTopics = existingTopics.subList(
                    existingTopics.size() - MAX_TOPICS, existingTopics.size());
        }
        profile.setTopTopics(toJsonArray(existingTopics));

        // Infer preferred tone from message length
        profile.setPreferredTone(inferTone(newAvg));

        profileRepository.save(profile);
    }

    /**
     * Get the user's profile (for prompt injection).
     */
    public UserInteractionProfile getProfile(String userId) {
        return profileRepository.findByUserId(userId).orElse(null);
    }

    private void updateActiveHours(UserInteractionProfile profile) {
        try {
            List<Integer> hours = parseJsonIntArray(profile.getPreferredActiveHours());
            // Ensure 24 slots
            while (hours.size() < 24) hours.add(0);

            int currentHour = LocalTime.now(ZoneOffset.UTC).getHour();
            hours.set(currentHour, hours.get(currentHour) + 1);

            profile.setPreferredActiveHours(objectMapper.writeValueAsString(hours));
        } catch (Exception e) {
            log.warn("Failed to update active hours: {}", e.getMessage());
        }
    }

    /**
     * Infer tone preference from average message length.
     * Short messages → CASUAL, Medium → PLAYFUL, Long → FORMAL.
     */
    private String inferTone(double avgLength) {
        if (avgLength < 10) return "CASUAL";
        if (avgLength < 30) return "PLAYFUL";
        return "FORMAL";
    }

    private List<String> parseJsonArray(String json) {
        try {
            return new ArrayList<>(objectMapper.readValue(json, new TypeReference<List<String>>() {}));
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    private List<Integer> parseJsonIntArray(String json) {
        try {
            return new ArrayList<>(objectMapper.readValue(json, new TypeReference<List<Integer>>() {}));
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    private String toJsonArray(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
