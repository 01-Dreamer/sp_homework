package com.zxylearn.demo1.eurekacart.dto;

import java.util.Map;

public record DiscoveredInstance(
        String serviceId,
        String host,
        int port,
        boolean secure,
        String uri,
        Map<String, String> metadata
) {
}
