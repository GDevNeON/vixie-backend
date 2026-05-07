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

/**
 * User preferences entity — currently stores the active companion selection.
 * Stored server-side for cross-device consistency (D-02).
 */
@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferences extends AuditableEntity {

    @Id
    @Column(nullable = false)
    private String id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(name = "active_character_id")
    private String activeCharacterId;

    @Builder.Default
    @Column(name = "voice_muted")
    private Boolean voiceMuted = false;

    @Builder.Default
    @Column(name = "voice_volume", columnDefinition = "numeric(3,2)")
    private Double voiceVolume = 1.0;
}
