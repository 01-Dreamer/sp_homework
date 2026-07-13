package com.zxylearn.demo3.inventory.controller;

import java.time.Instant;
import java.util.Map;

import com.zxylearn.demo3.inventory.dto.InventoryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/inventories")
public class InventoryController {

    private static final Map<String, Integer> STOCKS = Map.of(
            "3001", 120,
            "3002", 80,
            "3003", 35
    );

    private final int serverPort;

    public InventoryController(@Value("${server.port}") int serverPort) {
        this.serverPort = serverPort;
    }

    @GetMapping("/{productId}")
    public InventoryResponse getInventory(
            @PathVariable("productId") String productId,
            @RequestParam(name = "fail", defaultValue = "false") boolean fail,
            @RequestParam(name = "delayMillis", defaultValue = "0") long delayMillis
    ) throws InterruptedException {
        if (delayMillis > 0) {
            Thread.sleep(delayMillis);
        }
        if (fail) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Inventory service simulated failure");
        }
        return new InventoryResponse(
                productId,
                STOCKS.getOrDefault(productId, 0),
                "W-HZ-01",
                "inventory-service:" + serverPort,
                delayMillis,
                Instant.now()
        );
    }
}
