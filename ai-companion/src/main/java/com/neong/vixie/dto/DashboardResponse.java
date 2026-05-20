package com.neong.vixie.dto;

import java.util.List;

/**
 * Response DTO for creator dashboard data.
 */
public record DashboardResponse(
        int totalSales,
        int totalRevenue,
        List<ItemStats> items
) {
    public record ItemStats(
            String itemId,
            String itemName,
            int salesCount,
            int revenueCoins
    ) {}
}
