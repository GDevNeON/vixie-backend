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
 * Records a single purchase event for revenue tracking.
 * Each record represents one purchase of a creator's item,
 * with the 70/30 revenue split pre-calculated.
 */
@Entity
@Table(name = "creator_sale_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatorSaleRecord extends AuditableEntity {

    @Id
    @Column(nullable = false, length = 64)
    private String id;

    @Column(name = "creator_id", nullable = false, length = 64)
    private String creatorId;

    @Column(name = "item_id", nullable = false, length = 64)
    private String itemId;

    @Column(name = "gross_coins", nullable = false)
    private Integer grossCoins;

    @Column(name = "creator_coins", nullable = false)
    private Integer creatorCoins;

    @Column(name = "platform_coins", nullable = false)
    private Integer platformCoins;

    @Column(name = "purchased_at", nullable = false)
    private Instant purchasedAt;
}
