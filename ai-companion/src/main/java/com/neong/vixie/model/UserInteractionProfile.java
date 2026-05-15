package com.neong.vixie.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Tracks a user's conversation patterns and preferences over time.
 * Used to personalize the AI character's response style.
 *
 * Phase 8: Updated per WebSocket session disconnect. Fields feed into
 * CharacterPromptService.buildSystemPrompt() personalization clause.
 */
@Entity
@Table(name = "user_interaction_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInteractionProfile extends AuditableEntity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    /** Rolling average of user message word count. */
    @Column(name = "avg_message_length", nullable = false)
    @Builder.Default
    private double avgMessageLength = 0.0;

    /** JSON array of 24 integers representing hourly interaction counts. */
    @Column(name = "preferred_active_hours", nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String preferredActiveHours = "[]";

    /** JSON array of topic strings extracted from conversation summaries. */
    @Column(name = "top_topics", nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String topTopics = "[]";

    /** Inferred preferred tone: CASUAL, FORMAL, PLAYFUL. */
    @Column(name = "preferred_tone", nullable = false, length = 32)
    @Builder.Default
    private String preferredTone = "CASUAL";

    /** Aggregate of explicit feedback signals (future: thumbs up/down). */
    @Column(name = "explicit_feedback_score", nullable = false)
    @Builder.Default
    private double explicitFeedbackScore = 0.0;

    @Column(name = "total_session_count", nullable = false)
    @Builder.Default
    private int totalSessionCount = 0;

    @Column(name = "total_message_count", nullable = false)
    @Builder.Default
    private int totalMessageCount = 0;

    @Column(name = "last_session_at")
    private Instant lastSessionAt;
}
