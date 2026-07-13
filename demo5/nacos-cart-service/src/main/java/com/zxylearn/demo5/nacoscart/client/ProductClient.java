package com.zxylearn.demo5.nacoscart.client;

import com.zxylearn.demo5.nacoscart.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "nacos-product-service", path = "/api/products")
public interface ProductClient {

    @GetMapping("/{productId}")
    ProductResponse getProduct(@PathVariable("productId") String productId);
}
