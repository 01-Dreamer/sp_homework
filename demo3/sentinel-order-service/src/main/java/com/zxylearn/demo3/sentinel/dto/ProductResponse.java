package com.zxylearn.demo3.sentinel.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        String productId,
        String name,
        BigDecimal price,
        String servedBy,
        long delayMillis,
        Instant checkedAt
) {
}
