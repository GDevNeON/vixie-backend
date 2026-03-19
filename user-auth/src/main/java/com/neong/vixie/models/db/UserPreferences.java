package com.neong.vixie.models.db;

import com.neong.vixie.helpers.api.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferences extends AuditableEntity {

    @Id
    @Column(name = "preference_id", length = 64, nullable = false, updatable = false)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false, unique = true)
    private User user;

    @Builder.Default
    @Column(name = "locale", length = 10)
    private String locale = "en";

    @Column(name = "timezone", length = 50)
    private String timezone;

    @Builder.Default
    @Column(name = "theme_mode", length = 10)
    private String themeMode = "system";

    @Builder.Default
    @Column(name = "notification_push")
    private Boolean notificationPush = true;

    @Builder.Default
    @Column(name = "notification_email")
    private Boolean notificationEmail = true;
}
