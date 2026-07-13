package com.zxylearn.demo3.sentinel.controller;

import java.time.Instant;
import java.util.Map;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.zxylearn.demo3.sentinel.client.InventoryClient;
import com.zxylearn.demo3.sentinel.client.ProductClient;
import com.zxylearn.demo3.sentinel.config.SentinelRuleConfig;
import com.zxylearn.demo3.sentinel.dto.InventoryResponse;
import com.zxylearn.demo3.sentinel.dto.ProductResponse;
import com.zxylearn.demo3.sentinel.dto.ProtectedCallResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sentinel-orders")
public class SentinelOrderController {

    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final int serverPort;

    public SentinelOrderController(
            ProductClient productClient,
            InventoryClient inventoryClient,
            @Value("${server.port}") int serverPort
    ) {
        this.productClient = productClient;
        this.inventoryClient = inventoryClient;
        this.serverPort = serverPort;
    }

    @GetMapping("/circuit-a/products/{productId}")
    @SentinelResource(
            value = SentinelRuleConfig.CIRCUIT_A_RESOURCE,
            entryType = EntryType.OUT,
            fallback = "productFallback",
            blockHandler = "productBlock"
    )
    public ProtectedCallResponse circuitBreakerA(
            @PathVariable("productId") String productId,
            @RequestParam(name = "fail", defaultValue = "false") boolean fail,
            @RequestParam(name = "delayMillis", defaultValue = "0") long delayMillis
    ) {
        ProductResponse product = productClient.getProduct(productId, fail, delayMillis);
        return success("Sentinel：失败比例熔断", SentinelRuleConfig.CIRCUIT_A_RESOURCE, product);
    }

    @GetMapping("/circuit-b/inventories/{productId}")
    @SentinelResource(
            value = SentinelRuleConfig.CIRCUIT_B_RESOURCE,
            entryType = EntryType.OUT,
            fallback = "inventoryFallback",
            blockHandler = "inventoryBlock"
    )
    public ProtectedCallResponse circuitBreakerB(
            @PathVariable("productId") String productId,
            @RequestParam(name = "fail", defaultValue = "false") boolean fail,
            @RequestParam(name = "delayMillis", defaultValue = "0") long delayMillis
    ) {
        InventoryResponse inventory = inventoryClient.getInventory(productId, fail, delayMillis);
        return success("Sentinel：慢调用比例熔断", SentinelRuleConfig.CIRCUIT_B_RESOURCE, inventory);
    }

    @GetMapping("/bulkhead/inventories/{productId}")
    @SentinelResource(
            value = SentinelRuleConfig.BULKHEAD_RESOURCE,
            entryType = EntryType.OUT,
            fallback = "inventoryFallback",
            blockHandler = "inventoryBlock"
    )
    public ProtectedCallResponse bulkheadInventory(
            @PathVariable("productId") String productId,
            @RequestParam(name = "fail", defaultValue = "false") boolean fail,
            @RequestParam(name = "delayMillis", defaultValue = "1000") long delayMillis
    ) {
        InventoryResponse inventory = inventoryClient.getInventory(productId, fail, delayMillis);
        return success("Sentinel：线程数隔离", SentinelRuleConfig.BULKHEAD_RESOURCE, inventory);
    }

    @GetMapping("/rate-limit/products/{productId}")
    @SentinelResource(
            value = SentinelRuleConfig.RATE_LIMIT_RESOURCE,
            entryType = EntryType.OUT,
            fallback = "productFallback",
            blockHandler = "productBlock"
    )
    public ProtectedCallResponse rateLimitProduct(
            @PathVariable("productId") String productId,
            @RequestParam(name = "fail", defaultValue = "false") boolean fail,
            @RequestParam(name = "delayMillis", defaultValue = "0") long delayMillis
    ) {
        ProductResponse product = productClient.getProduct(productId, fail, delayMillis);
        return success("Sentinel：QPS 限流", SentinelRuleConfig.RATE_LIMIT_RESOURCE, product);
    }

    @GetMapping("/hotspot/products/{productId}")
    public ProtectedCallResponse hotspotProduct(
            @PathVariable("productId") String productId,
            @RequestParam(name = "fail", defaultValue = "false") boolean fail,
            @RequestParam(name = "delayMillis", defaultValue = "0") long delayMillis
    ) {
        Entry entry = null;
        try {
            entry = SphU.entry(SentinelRuleConfig.HOTSPOT_RESOURCE, EntryType.OUT, 1, productId);
            ProductResponse product = productClient.getProduct(productId, fail, delayMillis);
            return success("Sentinel：热点参数限流", SentinelRuleConfig.HOTSPOT_RESOURCE, product);
        } catch (BlockException exception) {
            return fallback("Sentinel 热点参数 blockHandler", SentinelRuleConfig.HOTSPOT_RESOURCE, exception);
        } catch (RuntimeException exception) {
            return fallback("Sentinel 热点参数 fallback", SentinelRuleConfig.HOTSPOT_RESOURCE, exception);
        } finally {
            if (entry != null) {
                entry.exit(1, productId);
            }
        }
    }

    @GetMapping("/sentinel/rules")
    public Map<String, Object> sentinelRules() {
        return Map.of(
                "degradeRules", DegradeRuleManager.getRules(),
                "flowRules", FlowRuleManager.getRules(),
                "paramFlowRules", ParamFlowRuleManager.getRules()
        );
    }

    public ProtectedCallResponse productFallback(String productId, boolean fail, long delayMillis, Throwable throwable) {
        return fallback("Sentinel fallback", "product remote call", throwable);
    }

    public ProtectedCallResponse inventoryFallback(String productId, boolean fail, long delayMillis, Throwable throwable) {
        return fallback("Sentinel fallback", "inventory remote call", throwable);
    }

    public ProtectedCallResponse productBlock(String productId, boolean fail, long delayMillis, BlockException exception) {
        return fallback("Sentinel blockHandler", exception.getRule().getResource(), exception);
    }

    public ProtectedCallResponse inventoryBlock(String productId, boolean fail, long delayMillis, BlockException exception) {
        return fallback("Sentinel blockHandler", exception.getRule().getResource(), exception);
    }

    private ProtectedCallResponse success(String scenario, String protectedBy, Object remoteResult) {
        return new ProtectedCallResponse(
                scenario,
                protectedBy,
                false,
                "remote call success",
                remoteResult,
                "sentinel-order-service:" + serverPort,
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
                "sentinel-order-service:" + serverPort,
                Instant.now()
        );
    }
}
