package com.zxylearn.demo2.product.dto;

import java.math.BigDecimal;

public record ProductRequest(String productId, String name, BigDecimal price) {
}
