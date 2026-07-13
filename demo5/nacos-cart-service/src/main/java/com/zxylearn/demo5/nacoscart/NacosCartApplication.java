package com.zxylearn.demo5.nacoscart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@EnableDiscoveryClient
@ConfigurationPropertiesScan
@SpringBootApplication
public class NacosCartApplication {

    public static void main(String[] args) {
        SpringApplication.run(NacosCartApplication.class, args);
    }
}
