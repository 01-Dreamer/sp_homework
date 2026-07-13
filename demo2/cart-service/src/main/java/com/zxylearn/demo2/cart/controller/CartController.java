package com.zxylearn.demo2.cart.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import com.zxylearn.demo2.cart.client.ProductFeignClient;
import com.zxylearn.demo2.cart.dto.CartResponse;
import com.zxylearn.demo2.cart.dto.DeleteResponse;
import com.zxylearn.demo2.cart.dto.DiscoveredInstance;
import com.zxylearn.demo2.cart.dto.InstanceResponse;
import com.zxylearn.demo2.cart.dto.ProductRequest;
import com.zxylearn.demo2.cart.dto.ProductResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/carts")
public class CartController {

    private final RestTemplate restTemplate;
    private final ProductFeignClient productFeignClient;
    private final DiscoveryClient discoveryClient;
    private final String productServiceId;
    private final int serverPort;

    public CartController(
            RestTemplate restTemplate,
            ProductFeignClient productFeignClient,
            DiscoveryClient discoveryClient,
            @Value("${demo2.product-service-id}") String productServiceId,
            @Value("${server.port}") int serverPort
    ) {
        this.restTemplate = restTemplate;
        this.productFeignClient = productFeignClient;
        this.discoveryClient = discoveryClient;
        this.productServiceId = productServiceId;
        this.serverPort = serverPort;
    }

    @GetMapping("/rest/products/{productId}")
    public CartResponse restTemplateGet(@PathVariable("productId") String productId) {
        ProductResponse product = restTemplate.getForObject(
                productBaseUrl() + "/{productId}",
                ProductResponse.class,
                productId
        );
        return toCartResponse(product, "RestTemplate GET");
    }

    @PostMapping("/rest/products")
    public ProductResponse restTemplatePost(@RequestBody ProductRequest request) {
        return restTemplate.postForObject(productBaseUrl(), request, ProductResponse.class);
    }

    @PutMapping("/rest/products/{productId}")
    public ProductResponse restTemplatePut(
            @PathVariable("productId") String productId,
            @RequestBody ProductRequest request
    ) {
        return restTemplate.exchange(
                productBaseUrl() + "/{productId}",
                HttpMethod.PUT,
                new HttpEntity<>(request),
                ProductResponse.class,
                productId
        ).getBody();
    }

    @DeleteMapping("/rest/products/{productId}")
    public DeleteResponse restTemplateDelete(@PathVariable("productId") String productId) {
        return restTemplate.exchange(
                productBaseUrl() + "/{productId}",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                DeleteResponse.class,
                productId
        ).getBody();
    }

    @GetMapping("/feign/products/{productId}")
    public CartResponse feignGet(@PathVariable("productId") String productId) {
        ProductResponse product = productFeignClient.getProduct(productId);
        return toCartResponse(product, "OpenFeign GET");
    }

    @PostMapping("/feign/products")
    public ProductResponse feignPost(@RequestBody ProductRequest request) {
        return productFeignClient.createProduct(request);
    }

    @PutMapping("/feign/products/{productId}")
    public ProductResponse feignPut(
            @PathVariable("productId") String productId,
            @RequestBody ProductRequest request
    ) {
        return productFeignClient.updateProduct(productId, request);
    }

    @DeleteMapping("/feign/products/{productId}")
    public DeleteResponse feignDelete(@PathVariable("productId") String productId) {
        return productFeignClient.deleteProduct(productId);
    }

    @GetMapping("/loadbalancer/rest")
    public List<InstanceResponse> restTemplateLoadBalancer(@RequestParam(name = "times", defaultValue = "8") int times) {
        return IntStream.range(0, normalizeTimes(times))
                .mapToObj(index -> restTemplate.getForObject(productBaseUrl() + "/ping", InstanceResponse.class))
                .toList();
    }

    @GetMapping("/loadbalancer/feign")
    public List<InstanceResponse> feignLoadBalancer(@RequestParam(name = "times", defaultValue = "8") int times) {
        return IntStream.range(0, normalizeTimes(times))
                .mapToObj(index -> productFeignClient.ping())
                .toList();
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

    @GetMapping("/services")
    public List<String> services() {
        return discoveryClient.getServices();
    }

    private String productBaseUrl() {
        return "http://" + productServiceId + "/api/products";
    }

    private CartResponse toCartResponse(ProductResponse product, String client) {
        return new CartResponse(
                UUID.randomUUID().toString(),
                product == null ? null : product.productId(),
                1,
                product,
                "cart-service:" + serverPort,
                client,
                Instant.now()
        );
    }

    private int normalizeTimes(int times) {
        return Math.max(1, Math.min(times, 20));
    }
}
