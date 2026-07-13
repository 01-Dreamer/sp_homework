package com.zxylearn.demo3.sentinel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
@EnableFeignClients
@SpringBootApplication
public class SentinelOrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SentinelOrderServiceApplication.class, args);
    }
}
