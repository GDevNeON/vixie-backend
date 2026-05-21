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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Stores FCM device tokens for push notification delivery.
 * Unique constraint on (userId, deviceId) enables upsert logic.
 */
@Entity
@Table(name = "notification_tokens",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "device_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationToken extends AuditableEntity {

    @Id
    @Column(nullable = false)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "fcm_token", nullable = false, length = 512)
    private String fcmToken;

    @Column(nullable = false, length = 20)
    private String platform;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @PrePersist
    public void generateId() {
        if (this.id == null) {
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            int number = ThreadLocalRandom.current().nextInt(0, 1_000_000);
            this.id = "notif_token_id_" + String.format("%06d", number) + "_" + today;
        }
    }
}
