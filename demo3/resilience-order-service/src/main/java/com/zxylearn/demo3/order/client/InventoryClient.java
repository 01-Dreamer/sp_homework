package com.zxylearn.demo3.order.client;

import com.zxylearn.demo3.order.dto.InventoryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "inventory-service", path = "/api/inventories")
public interface InventoryClient {

    @GetMapping("/{productId}")
    InventoryResponse getInventory(
            @PathVariable("productId") String productId,
            @RequestParam(name = "fail") boolean fail,
            @RequestParam(name = "delayMillis") long delayMillis
    );
}
