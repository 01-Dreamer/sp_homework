package com.zxylearn.demo5.configproduct;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@ConfigurationPropertiesScan
@SpringBootApplication
public class ConfigProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigProductApplication.class, args);
    }
}
