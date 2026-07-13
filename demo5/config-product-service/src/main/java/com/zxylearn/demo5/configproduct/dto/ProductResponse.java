package com.zxylearn.demo5.configproduct.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        String productId,
        String name,
        BigDecimal originalPrice,
        BigDecimal salePrice,
        String currency,
        String configMessage,
        String configVersion,
        String servedBy,
        Instant createdAt) {
}
