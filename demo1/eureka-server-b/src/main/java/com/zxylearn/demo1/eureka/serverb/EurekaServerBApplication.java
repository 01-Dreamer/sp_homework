package com.zxylearn.demo1.eureka.serverb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@EnableEurekaServer
@SpringBootApplication
public class EurekaServerBApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerBApplication.class, args);
    }
}
