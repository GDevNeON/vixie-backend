package com.neong.vixie.models.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "gacha_pull_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GachaPullRecord {

    @Id
    @Column(name = "pull_id", length = 64, nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            referencedColumnName = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_gacha_pull_user")
    )
    private User user;

    @Column(name = "banner_id", length = 64, nullable = false)
    private String bannerId;

    @Column(name = "item_id", length = 64, nullable = false)
    private String itemId;

    @Column(name = "rarity", length = 20, nullable = false)
    private String rarity;

    @Column(name = "pull_cost", nullable = false)
    private Integer pullCost;

    @Column(name = "pulled_at", nullable = false, updatable = false)
    private Instant pulledAt;
}
