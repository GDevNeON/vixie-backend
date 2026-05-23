package com.neong.vixie.service;

import com.neong.vixie.model.Banner;
import com.neong.vixie.model.BannerItem;
import com.neong.vixie.model.MarketplaceItem;
import com.neong.vixie.model.Rarity;
import com.neong.vixie.repository.BannerItemRepository;
import com.neong.vixie.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gacha probability engine with SecureRandom RNG.
 * Implements rarity weights, soft pity (75+), hard pity (100).
 *
 * Rarity base rates:
 *   Common: 60%
 *   Rare: 30%
 *   Epic: 7%
 *   Limited: 3% (when banner has limited items)
 *
 * Soft pity: from pull 75, Epic rate increases +2%/pull above 75.
 * Hard pity: at pull 100, guaranteed Epic.
 */
@Service
@RequiredArgsConstructor
public class GachaService {

    private static final Logger log = LoggerFactory.getLogger(GachaService.class);
    private static final SecureRandom rng = new SecureRandom();

    // Base drop rates (percentages)
    private static final double BASE_COMMON_RATE = 60.0;
    private static final double BASE_RARE_RATE = 30.0;
    private static final double BASE_EPIC_RATE = 7.0;
    private static final double BASE_LIMITED_RATE = 3.0;

    private static final int SOFT_PITY_START = 75;
    private static final int HARD_PITY = 100;
    private static final double SOFT_PITY_BONUS_PER_PULL = 2.0;

    private final BannerRepository bannerRepository;
    private final BannerItemRepository bannerItemRepository;
    private final UserAuthClient userAuthClient;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    /**
     * Get active banners.
     */
    public List<Banner> getActiveBanners() {
        return bannerRepository.findActiveBanners(Instant.now());
    }

    /**
     * Get banner by ID.
     */
    public Banner getBannerById(String bannerId) {
        return bannerRepository.findById(bannerId)
                .orElseThrow(() -> new IllegalArgumentException("Banner not found: " + bannerId));
    }

    /**
     * Execute gacha pull(s) for a user.
     * Determines items via RNG → then commits via user-auth internal API.
     *
     * @param userId   authenticated user
     * @param bannerId banner to pull from
     * @param count    1 or 10
     * @return list of pull results
     */
    public PullResponse pull(String userId, String bannerId, int count) {
        if (count != 1 && count != 10) {
            throw new IllegalArgumentException("Pull count must be 1 or 10");
        }

        String lockKey = "gacha:lock:" + userId;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", java.time.Duration.ofSeconds(10));
        if (acquired == null || !acquired) {
            throw new IllegalStateException("Please wait for your previous pull to complete");
        }

        try {
            Banner banner = getBannerById(bannerId);
            if (!Boolean.TRUE.equals(banner.getIsActive())) {
                throw new IllegalArgumentException("Banner is not active");
            }
            if (Instant.now().isBefore(banner.getStartDate()) || Instant.now().isAfter(banner.getEndDate())) {
                throw new IllegalArgumentException("Banner is not within active date range");
            }

            int totalCost = count == 1 ? banner.getPullCostOne() : banner.getPullCostTen();

            // Get pool items grouped by rarity
            List<BannerItem> poolItems = bannerItemRepository.findByBanner_Id(bannerId);
            if (poolItems.isEmpty()) {
                throw new IllegalArgumentException("Banner has no items in pool");
            }

            Map<Rarity, List<BannerItem>> itemsByRarity = poolItems.stream()
                    .collect(Collectors.groupingBy(bi -> bi.getItem().getRarity()));

            boolean hasLimited = itemsByRarity.containsKey(Rarity.LIMITED);

            // Get current pity
            int currentPity = userAuthClient.getPityCount(userId, bannerId);

            // Roll items
            List<PullResultItem> results = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                currentPity++;
                Rarity rolledRarity = rollRarity(currentPity, hasLimited);

                // If rolled rarity has no items in pool, downgrade to COMMON
                if (!itemsByRarity.containsKey(rolledRarity) || itemsByRarity.get(rolledRarity).isEmpty()) {
                    rolledRarity = Rarity.COMMON;
                }

                BannerItem selectedItem = selectItemFromRarity(itemsByRarity.get(rolledRarity));
                MarketplaceItem item = selectedItem.getItem();

                boolean isNew = true; // will be determined server-side during commit
                String rarityName = item.getRarity() != null ? item.getRarity().name() : Rarity.COMMON.name();
                results.add(new PullResultItem(item.getId(), item.getName(),
                        rarityName, item.getThumbnailUrl(), isNew));

                // Track pity locally for multi-pull accuracy
                if (rolledRarity == Rarity.EPIC) {
                    currentPity = 0;
                } else if (rolledRarity != Rarity.LIMITED) {
                    // Limited doesn't affect pity
                }
            }

            // Commit to user-auth (deduct coins, add inventory, update pity)
            List<Map<String, String>> commitEntries = results.stream()
                    .map(r -> Map.of("item_id", r.itemId(), "rarity", r.rarity()))
                    .toList();

            int newBalance = userAuthClient.commitPulls(userId, bannerId, totalCost, commitEntries);

            return new PullResponse(results, newBalance, totalCost);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * Roll rarity using weighted probability with pity system.
     */
    private Rarity rollRarity(int pullCount, boolean hasLimited) {
        // Hard pity
        if (pullCount >= HARD_PITY) {
            return Rarity.EPIC;
        }

        double epicRate = BASE_EPIC_RATE;
        double limitedRate = hasLimited ? BASE_LIMITED_RATE : 0.0;

        // Soft pity: boost Epic rate starting at pull 75
        if (pullCount > SOFT_PITY_START) {
            double bonus = (pullCount - SOFT_PITY_START) * SOFT_PITY_BONUS_PER_PULL;
            epicRate = Math.min(epicRate + bonus, 100.0 - limitedRate);
        }

        // Distribute remaining probability to Common and Rare
        double remainingRate = 100.0 - epicRate - limitedRate;
        double commonRate = remainingRate * (BASE_COMMON_RATE / (BASE_COMMON_RATE + BASE_RARE_RATE));
        double rareRate = remainingRate - commonRate;

        double roll = rng.nextDouble() * 100.0;

        if (roll < limitedRate) {
            return Rarity.LIMITED;
        } else if (roll < limitedRate + epicRate) {
            return Rarity.EPIC;
        } else if (roll < limitedRate + epicRate + rareRate) {
            return Rarity.RARE;
        } else {
            return Rarity.COMMON;
        }
    }

    /**
     * Select a specific item from a list of BannerItems using weighted selection.
     */
    private BannerItem selectItemFromRarity(List<BannerItem> items) {
        double totalWeight = items.stream().mapToDouble(BannerItem::getDropRateWeight).sum();
        double roll = rng.nextDouble() * totalWeight;

        double cumulative = 0.0;
        for (BannerItem item : items) {
            cumulative += item.getDropRateWeight();
            if (roll < cumulative) {
                return item;
            }
        }
        // Fallback to last item
        return items.get(items.size() - 1);
    }

    // DTOs
    public record PullResponse(List<PullResultItem> items, int newBalance, int totalCost) {}
    public record PullResultItem(String itemId, String name, String rarity, String thumbnailUrl, boolean isNew) {}
}
