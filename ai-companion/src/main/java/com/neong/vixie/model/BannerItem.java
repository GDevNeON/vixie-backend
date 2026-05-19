package com.neong.vixie.model;

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

/**
 * Join entity linking a Banner to a MarketplaceItem in the gacha pool.
 * isPoolExclusive = true means item only appears in this specific banner.
 * dropRateWeight controls relative probability within the item's rarity tier.
 */
@Entity
@Table(name = "banner_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BannerItem {

    @Id
    @Column(nullable = false, length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "banner_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_banner_item_banner")
    )
    private Banner banner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "item_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_banner_item_item")
    )
    private MarketplaceItem item;

    @Builder.Default
    @Column(name = "is_pool_exclusive", nullable = false)
    private Boolean isPoolExclusive = false;

    @Builder.Default
    @Column(name = "drop_rate_weight", nullable = false)
    private Double dropRateWeight = 1.0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
