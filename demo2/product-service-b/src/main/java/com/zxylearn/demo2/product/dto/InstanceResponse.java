package com.zxylearn.demo2.product.dto;

import java.time.Instant;

public record InstanceResponse(
        String serviceId,
        String instanceId,
        int port,
        String servedBy,
        String registry,
        Instant checkedAt
) {
}
