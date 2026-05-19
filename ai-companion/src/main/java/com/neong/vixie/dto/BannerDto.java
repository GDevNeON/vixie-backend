package com.neong.vixie.dto;

import com.neong.vixie.model.Banner;
import com.neong.vixie.model.BannerItem;

import java.time.Instant;
import java.util.List;

public record BannerDto(
        String id,
        String name,
        String description,
        String bannerImageUrl,
        Instant startDate,
        Instant endDate,
        boolean isActive,
        int pullCostOne,
        int pullCostTen,
        List<BannerItemDto> items
) {
    public static BannerDto from(Banner banner) {
        List<BannerItemDto> itemDtos = banner.getItems() != null
                ? banner.getItems().stream().map(BannerItemDto::from).toList()
                : List.of();
        return new BannerDto(
                banner.getId(),
                banner.getName(),
                banner.getDescription(),
                banner.getBannerImageUrl(),
                banner.getStartDate(),
                banner.getEndDate(),
                Boolean.TRUE.equals(banner.getIsActive()),
                banner.getPullCostOne(),
                banner.getPullCostTen(),
                itemDtos
        );
    }

    public record BannerItemDto(
            String itemId,
            String itemName,
            String rarity,
            String thumbnailUrl,
            boolean isPoolExclusive,
            double dropRateWeight
    ) {
        public static BannerItemDto from(BannerItem bi) {
            return new BannerItemDto(
                    bi.getItem().getId(),
                    bi.getItem().getName(),
                    bi.getItem().getRarity().name(),
                    bi.getItem().getThumbnailUrl(),
                    Boolean.TRUE.equals(bi.getIsPoolExclusive()),
                    bi.getDropRateWeight()
            );
        }
    }
}
