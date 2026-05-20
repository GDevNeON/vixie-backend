package com.neong.vixie.controller;

import com.neong.vixie.dto.CreatorProfileDto;
import com.neong.vixie.dto.CreatorRegistrationRequest;
import com.neong.vixie.dto.DashboardResponse;
import com.neong.vixie.model.CreatorProfile;
import com.neong.vixie.model.CreatorSaleRecord;
import com.neong.vixie.model.MarketplaceItem;
import com.neong.vixie.repository.CreatorSaleRecordRepository;
import com.neong.vixie.repository.MarketplaceItemRepository;
import com.neong.vixie.service.CreatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for creator operations.
 * All endpoints require authentication (secured in SecurityConfig).
 */
@RestController
@RequestMapping("/api/creator")
@RequiredArgsConstructor
public class CreatorController {

    private final CreatorService creatorService;
    private final MarketplaceItemRepository marketplaceItemRepository;
    private final CreatorSaleRecordRepository saleRecordRepository;

    /**
     * Register the current user as a creator.
     */
    @PostMapping("/register")
    public ResponseEntity<CreatorProfileDto> register(
            @Valid @RequestBody CreatorRegistrationRequest request,
            Principal principal) {
        String userId = principal.getName();

        if (!request.termsAccepted()) {
            throw new IllegalArgumentException("You must accept the creator terms to register");
        }

        CreatorProfile profile = creatorService.registerCreator(
                userId,
                request.displayName(),
                request.bio()
        );
        return ResponseEntity.ok(CreatorProfileDto.from(profile));
    }

    /**
     * Get the current user's creator profile.
     */
    @GetMapping("/profile")
    public ResponseEntity<CreatorProfileDto> getProfile(Principal principal) {
        String userId = principal.getName();
        return creatorService.getByUserId(userId)
                .map(CreatorProfileDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get creator dashboard with aggregate revenue data.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard(Principal principal) {
        String userId = principal.getName();

        CreatorProfile profile = creatorService.getByUserId(userId)
                .orElse(null);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }

        // Get all items by this creator
        List<MarketplaceItem> creatorItems = marketplaceItemRepository.findByCreatorId(profile.getId());

        // Get all sale records for this creator
        List<CreatorSaleRecord> saleRecords = saleRecordRepository.findByCreatorId(profile.getId());

        // Aggregate per-item stats
        Map<String, List<CreatorSaleRecord>> salesByItem = saleRecords.stream()
                .collect(Collectors.groupingBy(CreatorSaleRecord::getItemId));

        List<DashboardResponse.ItemStats> itemStats = creatorItems.stream()
                .map(item -> {
                    List<CreatorSaleRecord> itemSales = salesByItem.getOrDefault(item.getId(), List.of());
                    int salesCount = itemSales.size();
                    int revenue = itemSales.stream()
                            .mapToInt(CreatorSaleRecord::getCreatorCoins)
                            .sum();
                    return new DashboardResponse.ItemStats(
                            item.getId(),
                            item.getName(),
                            salesCount,
                            revenue
                    );
                })
                .toList();

        return ResponseEntity.ok(new DashboardResponse(
                profile.getTotalSales(),
                profile.getTotalRevenue(),
                itemStats
        ));
    }
}
