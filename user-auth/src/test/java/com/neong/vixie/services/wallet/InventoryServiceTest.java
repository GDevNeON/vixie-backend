package com.neong.vixie.services.wallet;

import com.neong.vixie.models.db.User;
import com.neong.vixie.models.db.UserInventory;
import com.neong.vixie.repositories.UserInventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private UserInventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private UserInventory testInventory;

    @BeforeEach
    void setUp() {
        testInventory = UserInventory.builder()
                .id("inv_123")
                .user(User.builder().id("user_1").build())
                .itemId("item_1")
                .acquiredAt(Instant.now())
                .build();
    }

    @Test
    void getOwnedItemIds_returnsListOfItemIds() {
        when(inventoryRepository.findByUser_Id("user_1")).thenReturn(List.of(testInventory));

        List<String> result = inventoryService.getOwnedItemIds("user_1");

        assertEquals(1, result.size());
        assertEquals("item_1", result.get(0));
    }

    @Test
    void ownsItem_owned_returnsTrue() {
        when(inventoryRepository.existsByUser_IdAndItemId("user_1", "item_1")).thenReturn(true);

        assertTrue(inventoryService.ownsItem("user_1", "item_1"));
    }

    @Test
    void ownsItem_notOwned_returnsFalse() {
        when(inventoryRepository.existsByUser_IdAndItemId("user_1", "item_2")).thenReturn(false);

        assertFalse(inventoryService.ownsItem("user_1", "item_2"));
    }
}
