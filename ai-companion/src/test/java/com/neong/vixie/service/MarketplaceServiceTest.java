package com.neong.vixie.service;

import com.neong.vixie.dto.MarketplaceItemDto;
import com.neong.vixie.model.ContentStatus;
import com.neong.vixie.model.MarketplaceItem;
import com.neong.vixie.model.Rarity;
import com.neong.vixie.repository.MarketplaceItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketplaceServiceTest {

    @Mock
    private MarketplaceItemRepository marketplaceItemRepository;

    @InjectMocks
    private MarketplaceService marketplaceService;

    private MarketplaceItem testItem;

    @BeforeEach
    void setUp() {
        testItem = new MarketplaceItem();
        testItem.setId("mktpl_id_123456_2026-05-17");
        testItem.setName("Test Item");
        testItem.setDescription("Test Description");
        testItem.setCreatorId("creator_1");
        testItem.setStatus(ContentStatus.PUBLISHED);
        testItem.setRarity(Rarity.RARE);
        testItem.setPriceCoins(100);
        testItem.setPriceFiat(BigDecimal.valueOf(1.99));
        testItem.setThumbnailUrl("http://example.com/thumb.png");
        testItem.setPreviewImageUrl("http://example.com/preview.png");
    }

    @Test
    void getItems_returnsPublishedItems() {
        Page<MarketplaceItem> page = new PageImpl<>(List.of(testItem));
        when(marketplaceItemRepository.findByStatus(ContentStatus.PUBLISHED, PageRequest.of(0, 10)))
                .thenReturn(page);

        Page<MarketplaceItemDto> result = marketplaceService.getItems(null, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Test Item", result.getContent().get(0).name());
    }

    @Test
    void getItemById_existingPublishedItem_returnsDto() {
        when(marketplaceItemRepository.findById(testItem.getId())).thenReturn(Optional.of(testItem));

        MarketplaceItemDto result = marketplaceService.getItemById(testItem.getId());

        assertNotNull(result);
        assertEquals(testItem.getId(), result.id());
    }

    @Test
    void getItemById_unpublishedItem_throwsException() {
        testItem.setStatus(ContentStatus.UNPUBLISHED);
        when(marketplaceItemRepository.findById(testItem.getId())).thenReturn(Optional.of(testItem));

        assertThrows(IllegalArgumentException.class, () -> marketplaceService.getItemById(testItem.getId()));
    }

    @Test
    void publishItem_authorizedCreator_updatesStatus() {
        testItem.setStatus(ContentStatus.UNPUBLISHED);
        when(marketplaceItemRepository.findById(testItem.getId())).thenReturn(Optional.of(testItem));
        when(marketplaceItemRepository.save(any(MarketplaceItem.class))).thenAnswer(i -> i.getArgument(0));

        MarketplaceItemDto result = marketplaceService.publishItem(testItem.getId(), "creator_1");

        assertEquals(ContentStatus.PUBLISHED, testItem.getStatus());
        assertEquals("Test Item", result.name());
    }

    @Test
    void publishItem_unauthorizedCreator_throwsException() {
        when(marketplaceItemRepository.findById(testItem.getId())).thenReturn(Optional.of(testItem));

        assertThrows(IllegalArgumentException.class, () -> marketplaceService.publishItem(testItem.getId(), "other_creator"));
    }
}
