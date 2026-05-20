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
 * Creator profile entity representing a user who has opted in as a content creator.
 * Stored in ai-companion service, keyed by userId.
 *
 * Phase 11: Tracks creator identity, verification status, and aggregate revenue stats.
 */
@Entity
@Table(name = "creator_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatorProfile extends AuditableEntity {

    @Id
    @Column(nullable = false, length = 64)
    private String id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "terms_accepted_at")
    private Instant termsAcceptedAt;

    @Builder.Default
    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    @Builder.Default
    @Column(name = "is_banned", nullable = false)
    private Boolean isBanned = false;

    @Builder.Default
    @Column(name = "total_sales", nullable = false)
    private Integer totalSales = 0;

    @Builder.Default
    @Column(name = "total_revenue", nullable = false)
    private Integer totalRevenue = 0;
}
