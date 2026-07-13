package com.zxylearn.demo3.order.controller;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.zxylearn.demo3.order.client.InventoryClient;
import com.zxylearn.demo3.order.client.ProductClient;
import com.zxylearn.demo3.order.dto.InventoryResponse;
import com.zxylearn.demo3.order.dto.ProductResponse;
import com.zxylearn.demo3.order.dto.ProtectedCallResponse;
import com.zxylearn.demo3.order.service.ProductCacheService;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final ProductCacheService productCacheService;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final BulkheadRegistry bulkheadRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;
    private final int serverPort;

    public OrderController(
            ProductClient productClient,
            InventoryClient inventoryClient,
            ProductCacheService productCacheService,
            CircuitBreakerRegistry circuitBreakerRegistry,
            BulkheadRegistry bulkheadRegistry,
            RateLimiterRegistry rateLimiterRegistry,
            @Value("${server.port}") int serverPort
    ) {
        this.productClient = productClient;
        this.inventoryClient = inventoryClient;
        this.productCacheService = productCacheService;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.bulkheadRegistry = bulkheadRegistry;
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.serverPort = serverPort;
    }

    @GetMapping("/circuit-a/products/{productId}")
    @CircuitBreaker(name = "circuitBreakerA", fallbackMethod = "productCircuitFallback")
    public ProtectedCallResponse circuitBreakerA(
            @PathVariable("productId") String productId,
            @RequestParam(name = "fail", defaultValue = "false") boolean fail,
            @RequestParam(name = "delayMillis", defaultValue = "0") long delayMillis
    ) {
        ProductResponse product = productClient.getProduct(productId, fail, delayMillis);
        return success("断路器A：失败率熔断", "circuitBreakerA", product);
    }

    @GetMapping("/circuit-b/inventories/{productId}")
    @CircuitBreaker(name = "circuitBreakerB", fallbackMethod = "inventoryCircuitFallback")
    public ProtectedCallResponse circuitBreakerB(
            @PathVariable("productId") String productId,
            @RequestParam(name = "fail", defaultValue = "false") boolean fail,
            @RequestParam(name = "delayMillis", defaultValue = "0") long delayMillis
    ) {
        InventoryResponse inventory = inventoryClient.getInventory(productId, fail, delayMillis);
        return success("断路器B：慢调用和失败率熔断", "circuitBreakerB", inventory);
    }

    @GetMapping("/bulkhead/inventories/{productId}")
    @Bulkhead(name = "inventoryBulkhead", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "inventoryBulkheadFallback")
    public ProtectedCallResponse bulkheadInventory(
            @PathVariable("productId") String productId,
            @RequestParam(name = "fail", defaultValue = "false") boolean fail,
            @RequestParam(name = "delayMillis", defaultValue = "1000") long delayMillis
    ) {
        InventoryResponse inventory = inventoryClient.getInventory(productId, fail, delayMillis);
        return success("隔离器：并发执行数量限制", "inventoryBulkhead", inventory);
    }

    @GetMapping("/rate-limit/products/{productId}")
    @RateLimiter(name = "productRateLimiter", fallbackMethod = "productRateLimiterFallback")
    public ProtectedCallResponse rateLimitProduct(
            @PathVariable("productId") String productId,
            @RequestParam(name = "fail", defaultValue = "false") boolean fail,
            @RequestParam(name = "delayMillis", defaultValue = "0") long delayMillis
    ) {
        ProductResponse product = productClient.getProduct(productId, fail, delayMillis);
        return success("限流器：2秒最多5个请求", "productRateLimiter", product);
    }

    @GetMapping("/cache/products/{productId}")
    public ProtectedCallResponse cachedProduct(
            @PathVariable("productId") String productId,
            @RequestParam(name = "fail", defaultValue = "false") boolean fail,
            @RequestParam(name = "delayMillis", defaultValue = "0") long delayMillis
    ) {
        ProductResponse product = productCacheService.getProduct(productId, fail, delayMillis);
        return success("结果缓存：按商品ID缓存远程调用结果", "productResultCache", product);
    }

    @DeleteMapping("/cache/products/{productId}")
    public Map<String, Object> evictCachedProduct(@PathVariable("productId") String productId) {
        productCacheService.evictProduct(productId);
        return Map.of(
                "evicted", true,
                "cacheName", "productResultCache",
                "cacheKey", productId,
                "servedBy", "resilience-order-service:" + serverPort,
                "checkedAt", Instant.now()
        );
    }

    @GetMapping("/timeout/inventories/{productId}")
    @TimeLimiter(name = "inventoryTimeout", fallbackMethod = "inventoryTimeoutFallback")
    public CompletableFuture<ProtectedCallResponse> timeoutInventory(
            @PathVariable("productId") String productId,
            @RequestParam(name = "fail", defaultValue = "false") boolean fail,
            @RequestParam(name = "delayMillis", defaultValue = "2000") long delayMillis
    ) {
        return CompletableFuture.supplyAsync(() -> {
            InventoryResponse inventory = inventoryClient.getInventory(productId, fail, delayMillis);
            return success("超时处理：1秒未返回则降级", "inventoryTimeout", inventory);
        });
    }

    @GetMapping("/resilience/status")
    public Map<String, Object> resilienceStatus() {
        return Map.of(
                "circuitBreakerA", circuitBreakerState("circuitBreakerA"),
                "circuitBreakerB", circuitBreakerState("circuitBreakerB"),
                "inventoryBulkhead", bulkheadState("inventoryBulkhead"),
                "productRateLimiter", rateLimiterState("productRateLimiter"),
                "productResultCache", Map.of(
                        "cacheName", "productResultCache",
                        "cacheKey", "productId",
                        "expireAfterWrite", "60s"
                ),
                "inventoryTimeout", Map.of(
                        "timeoutDuration", "1s",
                        "fallback", "inventoryTimeoutFallback"
                )
        );
    }

    public ProtectedCallResponse productCircuitFallback(
            String productId,
            boolean fail,
            long delayMillis,
            Throwable throwable
    ) {
        return fallback("断路器A：失败率熔断", "circuitBreakerA", throwable);
    }

    public ProtectedCallResponse inventoryCircuitFallback(
            String productId,
            boolean fail,
            long delayMillis,
            Throwable throwable
    ) {
        return fallback("断路器B：慢调用和失败率熔断", "circuitBreakerB", throwable);
    }

    public ProtectedCallResponse inventoryBulkheadFallback(
            String productId,
            boolean fail,
            long delayMillis,
            Throwable throwable
    ) {
        return fallback("隔离器：并发执行数量限制", "inventoryBulkhead", throwable);
    }

    public ProtectedCallResponse productRateLimiterFallback(
            String productId,
            boolean fail,
            long delayMillis,
            Throwable throwable
    ) {
        return fallback("限流器：2秒最多5个请求", "productRateLimiter", throwable);
    }

    public CompletableFuture<ProtectedCallResponse> inventoryTimeoutFallback(
            String productId,
            boolean fail,
            long delayMillis,
            Throwable throwable
    ) {
        return CompletableFuture.completedFuture(fallback("超时处理：1秒未返回则降级", "inventoryTimeout", throwable));
    }

    private ProtectedCallResponse success(String scenario, String protectedBy, Object remoteResult) {
        return new ProtectedCallResponse(
                scenario,
                protectedBy,
                false,
                "remote call success",
                remoteResult,
                "resilience-order-service:" + serverPort,
                Instant.now()
        );
    }

    private ProtectedCallResponse fallback(String scenario, String protectedBy, Throwable throwable) {
        return new ProtectedCallResponse(
                scenario,
                protectedBy,
                true,
                throwable.getClass().getSimpleName() + ": " + throwable.getMessage(),
                null,
                "resilience-order-service:" + serverPort,
                Instant.now()
        );
    }

    private Map<String, Object> circuitBreakerState(String name) {
        io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);
        return Map.of(
                "state", circuitBreaker.getState().name(),
                "failureRate", circuitBreaker.getMetrics().getFailureRate(),
                "slowCallRate", circuitBreaker.getMetrics().getSlowCallRate(),
                "failedCalls", circuitBreaker.getMetrics().getNumberOfFailedCalls(),
                "slowCalls", circuitBreaker.getMetrics().getNumberOfSlowCalls(),
                "notPermittedCalls", circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()
        );
    }

    private Map<String, Object> bulkheadState(String name) {
        io.github.resilience4j.bulkhead.Bulkhead bulkhead = bulkheadRegistry.bulkhead(name);
        return Map.of(
                "availableConcurrentCalls", bulkhead.getMetrics().getAvailableConcurrentCalls(),
                "maxAllowedConcurrentCalls", bulkhead.getMetrics().getMaxAllowedConcurrentCalls()
        );
    }

    private Map<String, Object> rateLimiterState(String name) {
        io.github.resilience4j.ratelimiter.RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(name);
        return Map.of(
                "availablePermissions", rateLimiter.getMetrics().getAvailablePermissions(),
                "waitingThreads", rateLimiter.getMetrics().getNumberOfWaitingThreads()
        );
    }
}
