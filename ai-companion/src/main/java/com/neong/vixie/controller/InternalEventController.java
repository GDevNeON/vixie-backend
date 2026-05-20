package com.neong.vixie.controller;

import com.neong.vixie.dto.PurchaseEventRequest;
import com.neong.vixie.service.RevenueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal controller for service-to-service events.
 * Secured via X-Service-Key header (not JWT) since calls come from user-auth.
 */
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
@Slf4j
public class InternalEventController {

    private final RevenueService revenueService;

    @Value("${internal.service-key:#{null}}")
    private String expectedServiceKey;

    /**
     * Process a purchase event from user-auth service.
     * Validates X-Service-Key header for service-to-service auth.
     */
    @PostMapping("/purchase-event")
    public ResponseEntity<Void> handlePurchaseEvent(
            @RequestBody PurchaseEventRequest request,
            @RequestHeader(value = "X-Service-Key", required = false) String serviceKey) {

        // Validate service key if configured
        if (expectedServiceKey != null && !expectedServiceKey.isEmpty()) {
            if (serviceKey == null || !serviceKey.equals(expectedServiceKey)) {
                log.warn("Rejected purchase event: invalid X-Service-Key");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        try {
            revenueService.processPurchaseEvent(
                    request.itemId(),
                    request.priceCoins(),
                    request.purchasedAt()
            );
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("Purchase event processing failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
