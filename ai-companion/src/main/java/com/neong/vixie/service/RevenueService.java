package com.neong.vixie.service;

import com.neong.vixie.model.CreatorProfile;
import com.neong.vixie.model.CreatorSaleRecord;
import com.neong.vixie.model.MarketplaceItem;
import com.neong.vixie.repository.CreatorProfileRepository;
import com.neong.vixie.repository.CreatorSaleRecordRepository;
import com.neong.vixie.repository.MarketplaceItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Processes purchase events and tracks creator revenue.
 * Revenue split: 70% creator, 30% platform.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RevenueService {

    private final MarketplaceItemRepository marketplaceItemRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final CreatorSaleRecordRepository saleRecordRepository;

    /**
     * Process a purchase event from user-auth service.
     * Creates a sale record and updates the creator's aggregate stats.
     *
     * @param itemId      the purchased item ID
     * @param priceCoins  the gross price in coins
     * @param purchasedAt when the purchase occurred
     */
    @Transactional
    public void processPurchaseEvent(String itemId, int priceCoins, Instant purchasedAt) {
        MarketplaceItem item = marketplaceItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));

        String creatorId = item.getCreatorId();
        if (creatorId == null) {
            log.info("Purchased item {} has no creator — skipping revenue tracking", itemId);
            return;
        }

        // 70/30 revenue split
        int creatorCoins = (int) (priceCoins * 0.70);
        int platformCoins = priceCoins - creatorCoins;

        // Create sale record
        CreatorSaleRecord record = CreatorSaleRecord.builder()
                .id(UUID.randomUUID().toString())
                .creatorId(creatorId)
                .itemId(itemId)
                .grossCoins(priceCoins)
                .creatorCoins(creatorCoins)
                .platformCoins(platformCoins)
                .purchasedAt(purchasedAt)
                .build();
        saleRecordRepository.save(record);

        // Update creator aggregate stats
        creatorProfileRepository.findById(creatorId).ifPresent(profile -> {
            profile.setTotalSales(profile.getTotalSales() + 1);
            profile.setTotalRevenue(profile.getTotalRevenue() + creatorCoins);
            creatorProfileRepository.save(profile);
        });

        log.info("Recorded sale: item={}, creator={}, gross={}, creatorShare={}", 
                itemId, creatorId, priceCoins, creatorCoins);
    }
}
