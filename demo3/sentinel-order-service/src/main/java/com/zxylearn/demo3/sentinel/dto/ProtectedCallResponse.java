package com.zxylearn.demo3.sentinel.dto;

import java.time.Instant;

public record ProtectedCallResponse(
        String scenario,
        String protectedBy,
        boolean fallback,
        String message,
        Object remoteResult,
        String servedBy,
        Instant createdAt
) {
}
