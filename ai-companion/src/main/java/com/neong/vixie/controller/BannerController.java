package com.neong.vixie.controller;

import com.neong.vixie.dto.BannerDto;
import com.neong.vixie.service.GachaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public banner endpoints — no auth required.
 */
@RestController
@RequiredArgsConstructor
public class BannerController {

    private final GachaService gachaService;

    @GetMapping("/api/gacha/banners")
    public ResponseEntity<List<BannerDto>> getActiveBanners() {
        List<BannerDto> banners = gachaService.getActiveBanners().stream()
                .map(BannerDto::from)
                .toList();
        return ResponseEntity.ok(banners);
    }

    @GetMapping("/api/gacha/banners/{id}")
    public ResponseEntity<BannerDto> getBannerById(@PathVariable String id) {
        return ResponseEntity.ok(BannerDto.from(gachaService.getBannerById(id)));
    }
}
