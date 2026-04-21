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
 * Per-user personality overrides for a character.
 * Each user can customize personality sliders (seriousness, energy, gentleness)
 * independently for each character they interact with.
 */
@Entity
@Table(name = "character_personalities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharacterPersonality extends AuditableEntity {

    @Id
    @Column(nullable = false)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "character_id", nullable = false)
    private String characterId;

    @Column(nullable = false)
    private Double seriousness;

    @Column(nullable = false)
    private Double energy;

    @Column(nullable = false)
    private Double gentleness;
}
