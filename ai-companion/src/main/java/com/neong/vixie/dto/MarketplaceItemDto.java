package com.neong.vixie.dto;

import com.neong.vixie.model.MarketplaceItem;
import com.neong.vixie.model.Rarity;

import java.math.BigDecimal;

public record MarketplaceItemDto(
        String id,
        String name,
        String description,
        Rarity rarity,
        Integer priceCoins,
        BigDecimal priceFiat,
        String thumbnailUrl,
        String previewImageUrl,
        String status
) {
    public static MarketplaceItemDto from(MarketplaceItem item) {
        return new MarketplaceItemDto(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getRarity(),
                item.getPriceCoins(),
                item.getPriceFiat(),
                item.getThumbnailUrl(),
                item.getPreviewImageUrl(),
                item.getStatus().name()
        );
    }
}
