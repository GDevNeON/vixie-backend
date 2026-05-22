package com.neong.vixie.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "character_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreferences extends AuditableEntity {

    public static final String DEFAULT_TIMEZONE = "Asia/Ho_Chi_Minh";
    public static final LocalTime DEFAULT_GREETING_TIME = LocalTime.of(8, 0);

    @Id
    @Column(nullable = false)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "character_id", nullable = false)
    private String characterId;

    @Builder.Default
    @Column(name = "greeting_enabled", nullable = false)
    private boolean greetingEnabled = true;

    @Builder.Default
    @Column(name = "greeting_time", nullable = false)
    private LocalTime greetingTime = DEFAULT_GREETING_TIME;

    @Builder.Default
    @Column(name = "focus_enabled", nullable = false)
    private boolean focusEnabled = false;

    @Column(name = "focus_time")
    private LocalTime focusTime;

    @Builder.Default
    @Column(name = "sleep_enabled", nullable = false)
    private boolean sleepEnabled = false;

    @Column(name = "sleep_time")
    private LocalTime sleepTime;

    @Builder.Default
    @Column(nullable = false, length = 64)
    private String timezone = DEFAULT_TIMEZONE;

    public static NotificationPreferences defaults(String userId, String characterId) {
        return NotificationPreferences.builder()
                .userId(userId)
                .characterId(characterId)
                .build();
    }

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = "notif_pref_" + UUID.randomUUID();
        }
        applyDefaults();
    }

    public void applyDefaults() {
        if (greetingTime == null) {
            greetingTime = DEFAULT_GREETING_TIME;
        }
        if (timezone == null || timezone.isBlank()) {
            timezone = DEFAULT_TIMEZONE;
        }
    }
}
