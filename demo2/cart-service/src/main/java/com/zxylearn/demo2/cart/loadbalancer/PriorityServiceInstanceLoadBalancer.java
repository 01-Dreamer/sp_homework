package com.zxylearn.demo2.cart.loadbalancer;

import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;

import reactor.core.publisher.Mono;

public class PriorityServiceInstanceLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;
    private final String serviceId;

    public PriorityServiceInstanceLoadBalancer(
            ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
            String serviceId
    ) {
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
        this.serviceId = serviceId;
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider.getIfAvailable();
        if (supplier == null) {
            return Mono.just(new EmptyResponse());
        }
        return supplier.get(request)
                .next()
                .map(this::selectInstance);
    }

    private Response<ServiceInstance> selectInstance(List<ServiceInstance> instances) {
        if (instances.isEmpty()) {
            return new EmptyResponse();
        }
        ServiceInstance selected = instances.stream()
                .min(Comparator
                        .comparingInt(this::priority)
                        .thenComparing(ServiceInstance::getHost)
                        .thenComparingInt(ServiceInstance::getPort))
                .orElse(null);
        return selected == null ? new EmptyResponse() : new DefaultResponse(selected);
    }

    private int priority(ServiceInstance instance) {
        String priority = instance.getMetadata().get("priority");
        if (priority == null) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(priority);
        } catch (NumberFormatException ex) {
            return Integer.MAX_VALUE;
        }
    }

    public String getServiceId() {
        return serviceId;
    }
}
