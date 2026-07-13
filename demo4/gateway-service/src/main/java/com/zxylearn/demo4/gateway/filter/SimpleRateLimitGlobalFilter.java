package com.zxylearn.demo4.gateway.filter;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class SimpleRateLimitGlobalFilter implements GlobalFilter, Ordered {

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final Duration window;

    public SimpleRateLimitGlobalFilter(
            @Value("${demo4.gateway.rate-limit.max-requests}") int maxRequests,
            @Value("${demo4.gateway.rate-limit.window}") Duration window
    ) {
        this.maxRequests = maxRequests;
        this.window = window;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (!path.startsWith("/api/") || exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        String key = clientKey(exchange);
        WindowCounter counter = counters.computeIfAbsent(key, ignored -> new WindowCounter(System.currentTimeMillis()));
        int remaining;
        synchronized (counter) {
            long now = System.currentTimeMillis();
            if (now - counter.windowStartMillis >= window.toMillis()) {
                counter.windowStartMillis = now;
                counter.count = 0;
            }
            counter.count++;
            remaining = Math.max(0, maxRequests - counter.count);
            if (counter.count > maxRequests) {
                return reject(exchange);
            }
        }

        exchange.getResponse().getHeaders().set("X-RateLimit-Limit", String.valueOf(maxRequests));
        exchange.getResponse().getHeaders().set("X-RateLimit-Remaining", String.valueOf(remaining));
        return chain.filter(exchange);
    }

    private Mono<Void> reject(ServerWebExchange exchange) {
        String body = """
                {"rateLimited":true,"message":"Gateway rate limit exceeded","limit":%d,"window":"%s","createdAt":"%s"}
                """.formatted(maxRequests, window, Instant.now()).trim();
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private String clientKey(ServerWebExchange exchange) {
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress == null || remoteAddress.getAddress() == null) {
            return "unknown";
        }
        return remoteAddress.getAddress().getHostAddress();
    }

    @Override
    public int getOrder() {
        return -90;
    }

    private static final class WindowCounter {
        private long windowStartMillis;
        private int count;

        private WindowCounter(long windowStartMillis) {
            this.windowStartMillis = windowStartMillis;
        }
    }
}
