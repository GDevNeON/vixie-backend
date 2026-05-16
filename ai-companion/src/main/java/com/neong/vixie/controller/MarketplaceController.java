package com.neong.vixie.controller;

import com.neong.vixie.dto.MarketplaceItemDto;
import com.neong.vixie.model.Rarity;
import com.neong.vixie.service.MarketplaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/marketplace/items")
@RequiredArgsConstructor
public class MarketplaceController {

    private final MarketplaceService marketplaceService;

    @GetMapping
    public ResponseEntity<Page<MarketplaceItemDto>> getItems(
            @RequestParam(required = false) Rarity rarity,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(marketplaceService.getItems(rarity, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MarketplaceItemDto> getItemById(@PathVariable String id) {
        return ResponseEntity.ok(marketplaceService.getItemById(id));
    }
}
