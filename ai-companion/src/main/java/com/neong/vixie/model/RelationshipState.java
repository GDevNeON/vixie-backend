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
 * Tracks the relationship level and XP between a user and a character.
 * Level 1-10, XP formula: xpToNextLevel = level * 100.
 * Level does NOT decrease in v1.
 */
@Entity
@Table(name = "relationship_states")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelationshipState extends AuditableEntity {

    @Id
    @Column(nullable = false)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "character_id", nullable = false)
    private String characterId;

    @Column(nullable = false)
    private Integer level;

    @Column(name = "current_xp", nullable = false)
    private Integer currentXp;

    @Column(name = "xp_to_next_level", nullable = false)
    private Integer xpToNextLevel;
}
