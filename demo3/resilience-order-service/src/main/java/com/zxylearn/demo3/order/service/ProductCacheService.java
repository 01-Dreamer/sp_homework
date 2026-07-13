package com.zxylearn.demo3.order.service;

import com.zxylearn.demo3.order.client.ProductClient;
import com.zxylearn.demo3.order.dto.ProductResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class ProductCacheService {

    private final ProductClient productClient;

    public ProductCacheService(ProductClient productClient) {
        this.productClient = productClient;
    }

    @Cacheable(cacheNames = "productResultCache", key = "#p0")
    public ProductResponse getProduct(String productId, boolean fail, long delayMillis) {
        return productClient.getProduct(productId, fail, delayMillis);
    }

    @CacheEvict(cacheNames = "productResultCache", key = "#p0")
    public void evictProduct(String productId) {
    }
}
