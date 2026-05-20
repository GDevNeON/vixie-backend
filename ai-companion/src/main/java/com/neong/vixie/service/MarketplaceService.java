package com.neong.vixie.service;

import com.neong.vixie.dto.MarketplaceItemDto;
import com.neong.vixie.model.ContentStatus;
import com.neong.vixie.model.Rarity;
import com.neong.vixie.repository.MarketplaceItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MarketplaceService {

    private final MarketplaceItemRepository marketplaceItemRepository;

    public Page<MarketplaceItemDto> getItems(Rarity rarity, Pageable pageable) {
        if (rarity != null) {
            return marketplaceItemRepository
                    .findByStatusAndRarity(ContentStatus.PUBLISHED, rarity, pageable)
                    .map(MarketplaceItemDto::from);
        }
        return marketplaceItemRepository
                .findByStatus(ContentStatus.PUBLISHED, pageable)
                .map(MarketplaceItemDto::from);
    }

    public MarketplaceItemDto getItemById(String id) {
        return marketplaceItemRepository.findById(id)
                .filter(item -> ContentStatus.PUBLISHED.equals(item.getStatus()))
                .map(MarketplaceItemDto::from)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + id));
    }
}
