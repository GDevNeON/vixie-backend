package com.neong.vixie.controllers.wallet;

import com.neong.vixie.services.wallet.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<List<String>> getInventory(
            @AuthenticationPrincipal UserDetails user) {
        List<String> itemIds = inventoryService.getOwnedItemIds(getUserId(user));
        return ResponseEntity.ok(itemIds);
    }

    private String getUserId(UserDetails user) {
        if (user instanceof com.neong.vixie.models.db.User u) {
            return u.getId();
        }
        throw new IllegalStateException("Unexpected UserDetails type");
    }
}
