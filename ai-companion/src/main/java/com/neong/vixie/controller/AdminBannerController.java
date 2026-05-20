package com.neong.vixie.controller;

import com.neong.vixie.dto.BannerDto;
import com.neong.vixie.model.Banner;
import com.neong.vixie.model.BannerItem;
import com.neong.vixie.model.MarketplaceItem;
import com.neong.vixie.repository.BannerItemRepository;
import com.neong.vixie.repository.BannerRepository;
import com.neong.vixie.repository.MarketplaceItemRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Admin CRUD for gacha banners — ADMIN role required.
 */
@RestController
@RequiredArgsConstructor
public class AdminBannerController {

    private final BannerRepository bannerRepository;
    private final BannerItemRepository bannerItemRepository;
    private final MarketplaceItemRepository marketplaceItemRepository;

    @PostMapping("/api/admin/gacha/banners")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<BannerDto> createBanner(@Valid @RequestBody CreateBannerRequest request) {
        Banner banner = Banner.builder()
                .id("banner_" + UUID.randomUUID().toString().substring(0, 8))
                .name(request.name())
                .description(request.description())
                .bannerImageUrl(request.bannerImageUrl())
                .startDate(request.startDate() != null ? request.startDate() : Instant.now())
                .endDate(request.endDate() != null ? request.endDate() : Instant.now().plus(java.time.Duration.ofDays(365)))
                .isActive(true)
                .pullCostOne(request.pullCostOne() != null ? request.pullCostOne() : 5)
                .pullCostTen(request.pullCostTen() != null ? request.pullCostTen() : 45)
                .build();
        bannerRepository.save(banner);
        return ResponseEntity.ok(BannerDto.from(banner));
    }

    @PutMapping("/api/admin/gacha/banners/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<BannerDto> updateBanner(
            @PathVariable String id,
            @Valid @RequestBody UpdateBannerRequest request) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Banner not found: " + id));

        if (request.name() != null) banner.setName(request.name());
        if (request.description() != null) banner.setDescription(request.description());
        if (request.bannerImageUrl() != null) banner.setBannerImageUrl(request.bannerImageUrl());
        if (request.startDate() != null) banner.setStartDate(request.startDate());
        if (request.endDate() != null) banner.setEndDate(request.endDate());
        if (request.isActive() != null) banner.setIsActive(request.isActive());
        if (request.pullCostOne() != null) banner.setPullCostOne(request.pullCostOne());
        if (request.pullCostTen() != null) banner.setPullCostTen(request.pullCostTen());

        bannerRepository.save(banner);
        return ResponseEntity.ok(BannerDto.from(banner));
    }

    @DeleteMapping("/api/admin/gacha/banners/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteBanner(@PathVariable String id) {
        bannerRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/admin/gacha/banners/{id}/items")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> addItemToBanner(
            @PathVariable String id,
            @Valid @RequestBody AddBannerItemRequest request) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Banner not found: " + id));
        MarketplaceItem item = marketplaceItemRepository.findById(request.itemId())
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + request.itemId()));

        BannerItem bannerItem = BannerItem.builder()
                .id("bi_" + UUID.randomUUID().toString().substring(0, 8))
                .banner(banner)
                .item(item)
                .isPoolExclusive(request.isPoolExclusive() != null ? request.isPoolExclusive() : false)
                .dropRateWeight(request.dropRateWeight() != null ? request.dropRateWeight() : 1.0)
                .createdAt(Instant.now())
                .build();
        bannerItemRepository.save(bannerItem);
        return ResponseEntity.ok().build();
    }

    // Request records
    public record CreateBannerRequest(
            @NotBlank @com.fasterxml.jackson.annotation.JsonProperty("name") String name,
            @com.fasterxml.jackson.annotation.JsonProperty("description") String description,
            @com.fasterxml.jackson.annotation.JsonProperty("banner_image_url") String bannerImageUrl,
            @com.fasterxml.jackson.annotation.JsonProperty("start_date") Instant startDate,
            @com.fasterxml.jackson.annotation.JsonProperty("end_date") Instant endDate,
            @com.fasterxml.jackson.annotation.JsonProperty("pull_cost_one") Integer pullCostOne,
            @com.fasterxml.jackson.annotation.JsonProperty("pull_cost_ten") Integer pullCostTen
    ) {}

    public record UpdateBannerRequest(
            @com.fasterxml.jackson.annotation.JsonProperty("name") String name,
            @com.fasterxml.jackson.annotation.JsonProperty("description") String description,
            @com.fasterxml.jackson.annotation.JsonProperty("banner_image_url") String bannerImageUrl,
            @com.fasterxml.jackson.annotation.JsonProperty("start_date") Instant startDate,
            @com.fasterxml.jackson.annotation.JsonProperty("end_date") Instant endDate,
            @com.fasterxml.jackson.annotation.JsonProperty("is_active") Boolean isActive,
            @com.fasterxml.jackson.annotation.JsonProperty("pull_cost_one") Integer pullCostOne,
            @com.fasterxml.jackson.annotation.JsonProperty("pull_cost_ten") Integer pullCostTen
    ) {}

    public record AddBannerItemRequest(
            @NotBlank @com.fasterxml.jackson.annotation.JsonProperty("item_id") String itemId,
            @com.fasterxml.jackson.annotation.JsonProperty("is_pool_exclusive") Boolean isPoolExclusive,
            @com.fasterxml.jackson.annotation.JsonProperty("drop_rate_weight") Double dropRateWeight
    ) {}
}
