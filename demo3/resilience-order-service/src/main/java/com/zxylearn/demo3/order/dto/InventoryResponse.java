package com.zxylearn.demo3.order.dto;

import java.time.Instant;

public record InventoryResponse(
        String productId,
        int stock,
        String warehouse,
        String servedBy,
        long delayMillis,
        Instant checkedAt
) {
}
