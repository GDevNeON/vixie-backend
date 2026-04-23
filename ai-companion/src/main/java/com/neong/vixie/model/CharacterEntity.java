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
 * Character definition entity.
 * Characters are admin-created in v1 — no user-facing creation.
 */
@Entity
@Table(name = "characters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharacterEntity extends AuditableEntity {

    @Id
    @Column(nullable = false)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Column(name = "default_seriousness", nullable = false, columnDefinition = "numeric(3,2)")
    private Double defaultSeriousness;

    @Column(name = "default_energy", nullable = false, columnDefinition = "numeric(3,2)")
    private Double defaultEnergy;

    @Column(name = "default_gentleness", nullable = false, columnDefinition = "numeric(3,2)")
    private Double defaultGentleness;
}
