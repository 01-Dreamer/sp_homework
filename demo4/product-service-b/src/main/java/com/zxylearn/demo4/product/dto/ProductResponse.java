package com.zxylearn.demo4.product.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        String productId,
        String name,
        BigDecimal price,
        String servedBy,
        Instant checkedAt
) {
}
