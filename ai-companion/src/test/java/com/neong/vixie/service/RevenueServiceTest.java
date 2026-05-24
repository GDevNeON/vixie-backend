package com.neong.vixie.service;

import com.neong.vixie.model.CreatorProfile;
import com.neong.vixie.model.CreatorSaleRecord;
import com.neong.vixie.model.MarketplaceItem;
import com.neong.vixie.repository.CreatorProfileRepository;
import com.neong.vixie.repository.CreatorSaleRecordRepository;
import com.neong.vixie.repository.MarketplaceItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RevenueServiceTest {

    private MarketplaceItemRepository itemRepo;
    private CreatorProfileRepository profileRepo;
    private CreatorSaleRecordRepository saleRepo;
    private RevenueService revenueService;

    @BeforeEach
    void setUp() {
        itemRepo = Mockito.mock(MarketplaceItemRepository.class);
        profileRepo = Mockito.mock(CreatorProfileRepository.class);
        saleRepo = Mockito.mock(CreatorSaleRecordRepository.class);
        revenueService = new RevenueService(itemRepo, profileRepo, saleRepo);
    }

    @Test
    void testPurchaseEventSplitsRevenue7030() {
        MarketplaceItem item = new MarketplaceItem();
        item.setId("item1");
        item.setCreatorId("creator1");
        
        CreatorProfile profile = new CreatorProfile();
        profile.setId("creator1");
        profile.setTotalSales(0);
        profile.setTotalRevenue(0);
        
        when(itemRepo.findById("item1")).thenReturn(Optional.of(item));
        when(profileRepo.findById("creator1")).thenReturn(Optional.of(profile));
        
        revenueService.processPurchaseEvent("item1", 100, Instant.now());
        
        ArgumentCaptor<CreatorSaleRecord> saleCaptor = ArgumentCaptor.forClass(CreatorSaleRecord.class);
        verify(saleRepo).save(saleCaptor.capture());
        
        CreatorSaleRecord savedRecord = saleCaptor.getValue();
        assertEquals(100, savedRecord.getGrossCoins());
        assertEquals(70, savedRecord.getCreatorCoins(), "Creator receives 70%");
        assertEquals(30, savedRecord.getPlatformCoins(), "Platform receives 30%");
        
        ArgumentCaptor<CreatorProfile> profileCaptor = ArgumentCaptor.forClass(CreatorProfile.class);
        verify(profileRepo).save(profileCaptor.capture());
        
        CreatorProfile savedProfile = profileCaptor.getValue();
        assertEquals(1, savedProfile.getTotalSales());
        assertEquals(70, savedProfile.getTotalRevenue());
    }
}
