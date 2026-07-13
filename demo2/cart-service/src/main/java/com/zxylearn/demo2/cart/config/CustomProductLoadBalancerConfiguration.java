package com.zxylearn.demo2.cart.config;

import com.zxylearn.demo2.cart.client.ProductFeignClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("custom")
@Configuration(proxyBeanMethods = false)
@LoadBalancerClient(name = ProductFeignClient.SERVICE_NAME, configuration = PriorityLoadBalancerConfiguration.class)
public class CustomProductLoadBalancerConfiguration {
}
