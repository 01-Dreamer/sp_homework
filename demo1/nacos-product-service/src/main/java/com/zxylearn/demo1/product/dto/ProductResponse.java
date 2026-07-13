package com.zxylearn.demo1.product.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        String productId,
        String name,
        BigDecimal price,
        String servedBy,
        String registry,
        Instant checkedAt
) {
}
