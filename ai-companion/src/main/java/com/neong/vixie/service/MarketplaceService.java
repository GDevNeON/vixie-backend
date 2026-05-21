package com.neong.vixie.service;

import com.neong.vixie.dto.MarketplaceItemDto;
import com.neong.vixie.model.ContentStatus;
import com.neong.vixie.model.Rarity;
import com.neong.vixie.model.MarketplaceItem;
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

    public java.util.List<MarketplaceItemDto> getCreatorItems(String creatorId) {
        return marketplaceItemRepository.findByCreatorId(creatorId).stream()
                .map(MarketplaceItemDto::from)
                .toList();
    }

    public MarketplaceItemDto publishItem(String itemId, String creatorId) {
        MarketplaceItem item = marketplaceItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));
        if (!item.getCreatorId().equals(creatorId)) {
            throw new IllegalArgumentException("Not authorized to publish this item");
        }
        item.setStatus(ContentStatus.PUBLISHED);
        return MarketplaceItemDto.from(marketplaceItemRepository.save(item));
    }

    public MarketplaceItemDto unpublishItem(String itemId, String creatorId) {
        MarketplaceItem item = marketplaceItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));
        if (!item.getCreatorId().equals(creatorId)) {
            throw new IllegalArgumentException("Not authorized to unpublish this item");
        }
        item.setStatus(ContentStatus.UNPUBLISHED);
        return MarketplaceItemDto.from(marketplaceItemRepository.save(item));
    }

    public MarketplaceItemDto updateItemMetadata(String itemId, String creatorId, String name, String description, Integer priceCoins) {
        MarketplaceItem item = marketplaceItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));
        if (!item.getCreatorId().equals(creatorId)) {
            throw new IllegalArgumentException("Not authorized to update this item");
        }
        if (name != null) item.setName(name);
        if (description != null) item.setDescription(description);
        if (priceCoins != null) item.setPriceCoins(priceCoins);
        return MarketplaceItemDto.from(marketplaceItemRepository.save(item));
    }
}
