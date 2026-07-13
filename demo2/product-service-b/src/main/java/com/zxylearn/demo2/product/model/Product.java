package com.zxylearn.demo2.product.model;

import java.math.BigDecimal;

public record Product(String id, String name, BigDecimal price) {
}
