package com.neong.vixie.controller;

import com.neong.vixie.dto.CreatorProfileDto;
import com.neong.vixie.dto.CreatorRegistrationRequest;
import com.neong.vixie.dto.DashboardResponse;
import com.neong.vixie.dto.MarketplaceItemDto;
import com.neong.vixie.model.ContentStatus;
import com.neong.vixie.model.CreatorProfile;
import com.neong.vixie.model.CreatorSaleRecord;
import com.neong.vixie.model.MarketplaceItem;
import com.neong.vixie.model.Rarity;
import com.neong.vixie.repository.CreatorSaleRecordRepository;
import com.neong.vixie.repository.MarketplaceItemRepository;
import com.neong.vixie.service.CloudinaryStorageService;
import com.neong.vixie.service.CreatorService;
import com.neong.vixie.service.ModelValidationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * REST controller for creator operations.
 * All endpoints require authentication (secured in SecurityConfig).
 */
@RestController
@RequestMapping("/api/creator")
@RequiredArgsConstructor
@Slf4j
public class CreatorController {

    private final CreatorService creatorService;
    private final MarketplaceItemRepository marketplaceItemRepository;
    private final CreatorSaleRecordRepository saleRecordRepository;
    private final ModelValidationService modelValidationService;
    private final CloudinaryStorageService cloudinaryStorageService;

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

    /**
     * Upload a new content package (Live2D model zip).
     * Validates the zip, uploads to Cloudinary, creates a DRAFT MarketplaceItem.
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadContent(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam(value = "price_coins", required = false) Integer priceCoins,
            @RequestParam(value = "rarity", defaultValue = "COMMON") String rarity,
            Principal principal) {
        String userId = principal.getName();

        CreatorProfile profile = creatorService.getByUserId(userId)
                .orElse(null);
        if (profile == null) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "You must be a registered creator to upload content"));
        }

        // Validate the zip file
        ModelValidationService.ValidationResult validation;
        try {
            validation = modelValidationService.validate(file.getInputStream());
        } catch (Exception e) {
            log.error("Failed to validate uploaded file", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to read uploaded file", "details", List.of(e.getMessage())));
        }

        if (!validation.valid()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Validation failed", "details", validation.errors()));
        }

        // Upload extracted files to Cloudinary
        String uploadId = UUID.randomUUID().toString();
        String folderPath = "creators/" + profile.getId() + "/" + uploadId + "/";
        String thumbnailUrl = null;

        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String entryName = entry.getName();
                byte[] data = readZipEntryBytes(zis);
                String cdnUrl = cloudinaryStorageService.upload(data, folderPath, entryName);

                // Use first texture as thumbnail
                if (thumbnailUrl == null && (entryName.endsWith(".png") || entryName.endsWith(".jpg"))) {
                    thumbnailUrl = cdnUrl;
                }
                zis.closeEntry();
            }
        } catch (Exception e) {
            log.error("Failed to upload content to storage", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Storage upload failed"));
        }

        // Create DRAFT MarketplaceItem
        Rarity itemRarity;
        try {
            itemRarity = Rarity.valueOf(rarity.toUpperCase());
        } catch (IllegalArgumentException e) {
            itemRarity = Rarity.COMMON;
        }

        MarketplaceItem item = MarketplaceItem.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .description(description)
                .rarity(itemRarity)
                .priceCoins(priceCoins)
                .thumbnailUrl(thumbnailUrl)
                .previewImageUrl(thumbnailUrl)
                .status(ContentStatus.DRAFT)
                .creatorId(profile.getId())
                .build();
        marketplaceItemRepository.save(item);

        log.info("Creator {} uploaded new item {} (DRAFT)", profile.getId(), item.getId());
        return ResponseEntity.ok(MarketplaceItemDto.from(item));
    }

    private byte[] readZipEntryBytes(ZipInputStream zis) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        while ((len = zis.read(buffer)) > 0) {
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }
}
