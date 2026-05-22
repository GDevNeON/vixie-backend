package com.neong.vixie.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "user_occasions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserOccasion extends AuditableEntity {

    @Id
    @Column(nullable = false)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OccasionType type;

    @Column(nullable = false)
    private String label;

    @Column(name = "occasion_date", nullable = false, length = 5)
    private String occasionDate;

    @Builder.Default
    @Column(name = "notification_enabled", nullable = false)
    private boolean notificationEnabled = true;

    @Builder.Default
    @Column(name = "detected_from_chat", nullable = false)
    private boolean detectedFromChat = false;

    @Builder.Default
    @Column(name = "confirmed_by_user", nullable = false)
    private boolean confirmedByUser = false;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = "occasion_" + UUID.randomUUID();
        }
    }
}
