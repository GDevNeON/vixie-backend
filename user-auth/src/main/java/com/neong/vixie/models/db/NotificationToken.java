package com.neong.vixie.models.db;

import com.neong.vixie.helpers.api.AuditableEntity;
import com.neong.vixie.helpers.api.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Stores FCM device tokens for push notification delivery.
 * Unique constraint on (userId, deviceId) enables upsert logic.
 */
@Entity
@Table(name = "notification_tokens",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "device_id"}))
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
    private boolean isActive = true;

    public NotificationToken() {}

    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = IdGenerator.generateId("notif_token");
        }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
