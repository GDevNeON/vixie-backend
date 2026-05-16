package com.neong.vixie.services.wallet;

import com.neong.vixie.repositories.UserInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final UserInventoryRepository inventoryRepository;

    /**
     * Get list of item IDs owned by user.
     * Flutter can hydrate full item details from the catalog API.
     */
    public List<String> getOwnedItemIds(String userId) {
        return inventoryRepository.findByUser_Id(userId)
                .stream()
                .map(inv -> inv.getItemId())
                .toList();
    }

    public boolean ownsItem(String userId, String itemId) {
        return inventoryRepository.existsByUser_IdAndItemId(userId, itemId);
    }
}
