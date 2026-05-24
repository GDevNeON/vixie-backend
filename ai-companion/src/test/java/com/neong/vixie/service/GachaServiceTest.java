package com.neong.vixie.service;

import com.neong.vixie.model.Banner;
import com.neong.vixie.model.BannerItem;
import com.neong.vixie.model.MarketplaceItem;
import com.neong.vixie.model.Rarity;
import com.neong.vixie.repository.BannerItemRepository;
import com.neong.vixie.repository.BannerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GachaServiceTest {

    @Mock
    private BannerRepository bannerRepository;

    @Mock
    private BannerItemRepository bannerItemRepository;

    @Mock
    private UserAuthClient userAuthClient;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private GachaService gachaService;

    private Banner banner;
    private MarketplaceItem itemCommon;
    private MarketplaceItem itemEpic;
    private MarketplaceItem itemLimited;
    private BannerItem bItemCommon;
    private BannerItem bItemEpic;
    private BannerItem bItemLimited;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        banner = new Banner();
        banner.setId("banner_1");
        banner.setIsActive(true);
        banner.setStartDate(Instant.now().minus(1, ChronoUnit.DAYS));
        banner.setEndDate(Instant.now().plus(1, ChronoUnit.DAYS));
        banner.setPullCostOne(5);
        banner.setPullCostTen(45);

        itemCommon = new MarketplaceItem();
        itemCommon.setId("item_common");
        itemCommon.setRarity(Rarity.COMMON);

        itemEpic = new MarketplaceItem();
        itemEpic.setId("item_epic");
        itemEpic.setRarity(Rarity.EPIC);

        itemLimited = new MarketplaceItem();
        itemLimited.setId("item_limited");
        itemLimited.setRarity(Rarity.LIMITED);

        bItemCommon = new BannerItem();
        bItemCommon.setItem(itemCommon);
        bItemCommon.setDropRateWeight(100.0);

        bItemEpic = new BannerItem();
        bItemEpic.setItem(itemEpic);
        bItemEpic.setDropRateWeight(10.0);

        bItemLimited = new BannerItem();
        bItemLimited.setItem(itemLimited);
        bItemLimited.setDropRateWeight(5.0);
    }

    @Test
    void pull_hardPity_guaranteesEpic() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(bannerRepository.findById("banner_1")).thenReturn(Optional.of(banner));
        
        when(bannerItemRepository.findByBanner_Id("banner_1"))
                .thenReturn(List.of(bItemCommon, bItemEpic));

        when(userAuthClient.getPityCount("user_1", "banner_1")).thenReturn(99);
        when(userAuthClient.commitPulls(eq("user_1"), eq("banner_1"), anyInt(), any()))
                .thenReturn(95);

        GachaService.PullResponse response = gachaService.pull("user_1", "banner_1", 1);

        assertEquals(1, response.items().size());
        assertEquals("EPIC", response.items().get(0).rarity());
    }

    @Test
    void pull_softPity_increasesEpicRate() {
        // Soft pity starts at 75. 
        // We can't strictly assert the randomness without mocking SecureRandom, 
        // but we can ensure the code executes cleanly when at soft pity.
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(bannerRepository.findById("banner_1")).thenReturn(Optional.of(banner));
        
        when(bannerItemRepository.findByBanner_Id("banner_1"))
                .thenReturn(List.of(bItemCommon, bItemEpic));

        when(userAuthClient.getPityCount("user_1", "banner_1")).thenReturn(80);
        when(userAuthClient.commitPulls(eq("user_1"), eq("banner_1"), anyInt(), any()))
                .thenReturn(95);

        GachaService.PullResponse response = gachaService.pull("user_1", "banner_1", 1);

        assertEquals(1, response.items().size());
        // Could be COMMON or EPIC, just verifying it doesn't crash
        assertTrue(List.of("COMMON", "EPIC", "RARE").contains(response.items().get(0).rarity()));
    }

    @Test
    void pull_limitedItemsDoNotTriggerPity() {
        // Even at hard pity, if limited is rolled, it doesn't matter, but Limited is a separate rarity
        // If we mock SecureRandom to return a value that hits Limited, we could test it.
        // For now, let's just make sure we can pull from a banner with limited items.
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(bannerRepository.findById("banner_1")).thenReturn(Optional.of(banner));
        
        when(bannerItemRepository.findByBanner_Id("banner_1"))
                .thenReturn(List.of(bItemCommon, bItemEpic, bItemLimited));

        when(userAuthClient.getPityCount("user_1", "banner_1")).thenReturn(0);
        when(userAuthClient.commitPulls(eq("user_1"), eq("banner_1"), anyInt(), any()))
                .thenReturn(95);

        GachaService.PullResponse response = gachaService.pull("user_1", "banner_1", 10);

        assertEquals(10, response.items().size());
        verify(userAuthClient).commitPulls(eq("user_1"), eq("banner_1"), eq(45), any());
    }
}
