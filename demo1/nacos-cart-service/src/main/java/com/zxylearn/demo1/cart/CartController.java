package com.zxylearn.demo1.cart;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/carts")
public class CartController {

    private final RestClient restClient;
    private final DiscoveryClient discoveryClient;
    private final LoadBalancerClient loadBalancerClient;
    private final int serverPort;

    public CartController(
            DiscoveryClient discoveryClient,
            LoadBalancerClient loadBalancerClient,
            @Value("${server.port}") int serverPort
    ) {
        this.restClient = RestClient.builder().build();
        this.discoveryClient = discoveryClient;
        this.loadBalancerClient = loadBalancerClient;
        this.serverPort = serverPort;
    }

    @GetMapping("/{productId}")
    public CartResponse addProduct(@PathVariable("productId") String productId) {
        ServiceInstance instance = loadBalancerClient.choose("nacos-product-service");
        if (instance == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No nacos-product-service instance available");
        }

        ProductResponse product = restClient.get()
                .uri(instance.getUri() + "/api/products/{productId}", productId)
                .retrieve()
                .body(ProductResponse.class);

        return new CartResponse(
                UUID.randomUUID().toString(),
                productId,
                1,
                product == null ? BigDecimal.ZERO : product.price(),
                product,
                "nacos-cart-service:" + serverPort,
                "nacos",
                Instant.now()
        );
    }

    @GetMapping("/discovery/{serviceId}")
    public List<DiscoveredInstance> discovery(@PathVariable("serviceId") String serviceId) {
        return discoveryClient.getInstances(serviceId).stream()
                .map(instance -> new DiscoveredInstance(
                        instance.getServiceId(),
                        instance.getHost(),
                        instance.getPort(),
                        instance.isSecure(),
                        instance.getUri().toString(),
                        instance.getMetadata()
                ))
                .toList();
    }

    public record CartResponse(
            String cartId,
            String productId,
            int quantity,
            BigDecimal totalAmount,
            ProductResponse product,
            String servedBy,
            String registry,
            Instant createdAt
    ) {
    }

    public record ProductResponse(
            String productId,
            String name,
            BigDecimal price,
            String servedBy,
            String registry,
            Instant checkedAt
    ) {
    }

    public record DiscoveredInstance(
            String serviceId,
            String host,
            int port,
            boolean secure,
            String uri,
            Map<String, String> metadata
    ) {
    }
}
