package com.neong.vixie.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Marketplace item entity representing a purchasable content item.
 * Stored in ai-companion service (content catalog).
 */
@Entity
@Table(name = "marketplace_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketplaceItem extends AuditableEntity {

    @Id
    @Column(nullable = false, length = 64)
    private String id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rarity rarity;

    @Column(name = "price_coins")
    private Integer priceCoins;

    @Column(name = "price_fiat", precision = 10, scale = 2)
    private BigDecimal priceFiat;

    @Column(name = "thumbnail_url", length = 2048)
    private String thumbnailUrl;

    @Column(name = "preview_image_url", length = 2048)
    private String previewImageUrl;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private ContentStatus status = ContentStatus.PUBLISHED;

    @Column(name = "creator_id", length = 64)
    private String creatorId;
}
