# 软件服务工程实验二报告说明

本工程用于完成“远程服务调用、OpenFeign、客户端负载均衡”实验。工程继续沿用实验一的商品服务和购物车服务命名，只使用 Nacos 作为注册中心。

GitHub 地址：`https://github.com/01-Dreamer/sp_homework`

## 一、工程模块

| 模块 | 端口 | 作用 | 注册到 Nacos 的服务名 |
| --- | ---: | --- | --- |
| `product-service-a` | 8091 | 商品服务提供者实例 A | `product-service` |
| `product-service-b` | 8093 | 商品服务提供者实例 B | `product-service` |
| `cart-service` | 8092 | 购物车服务消费者 | `cart-service` |

Nacos 配置：

- 控制台：`http://SERVER_IP_PLACEHOLDER:8848/nacos`
- 用户名：`nacos`
- 密码：`20230607`
- 服务分组：`DEMO2`
- 客户端注册地址：`SERVER_IP_PLACEHOLDER:8848`

## 二、版本与依赖

父工程 [pom.xml](pom.xml) 使用 `dependencyManagement` 统一管理版本：

```xml
<java.version>21</java.version>
<spring.boot.version>4.1.0</spring.boot.version>
<spring.cloud.version>2025.1.2</spring.cloud.version>
<spring.cloud.alibaba.version>2025.1.0.0</spring.cloud.alibaba.version>
```

消费者 `cart-service` 引入：

- `spring-cloud-starter-alibaba-nacos-discovery`
- `spring-cloud-starter-loadbalancer`
- `spring-cloud-starter-openfeign`
- `spring-boot-starter-webmvc`

## 三、启动命令

先编译：

```bash
./mvnw clean package -DskipTests
```

启动两个商品服务实例：

```bash
./mvnw -pl product-service-a spring-boot:run
./mvnw -pl product-service-b spring-boot:run
```

启动购物车服务，默认使用 Spring Cloud LoadBalancer 轮询策略：

```bash
./mvnw -pl cart-service spring-boot:run
```

启动购物车服务并启用随机负载均衡策略：

```bash
./mvnw -pl cart-service spring-boot:run -Dspring-boot.run.profiles=random
```

启动购物车服务并启用选做题的自定义优先级负载均衡策略：

```bash
./mvnw -pl cart-service spring-boot:run -Dspring-boot.run.profiles=custom
```

如果需要临时修改 Nacos 地址或账号密码：

```bash
NACOS_SERVER_ADDR=SERVER_IP_PLACEHOLDER:8848 NACOS_USERNAME=nacos NACOS_PASSWORD=20230607 \
./mvnw -pl cart-service spring-boot:run
```

## 四、关键实验步骤

### 4.1 实验项目概述

本实验项目是一个基于 Spring Cloud 的微服务远程调用示例工程，主要用于验证 RestTemplate、OpenFeign 和 Spring Cloud LoadBalancer 的使用方式。项目采用 Maven 多模块结构，包含两个商品服务提供者实例 `product-service-a`、`product-service-b` 和一个购物车服务消费者 `cart-service`。两个商品服务实例使用相同服务名 `product-service` 注册到 Nacos，购物车服务通过服务发现按服务名调用商品服务，从而模拟真实微服务系统中“一个消费者调用多个提供者实例”的场景。

实验中，商品服务提供 GET、POST、PUT、DELETE 四类 RESTful 接口；购物车服务分别使用 RestTemplate 和 OpenFeign 完成远程调用。负载均衡部分先展示 Spring Cloud LoadBalancer 默认轮询策略，再通过 `random` profile 展示随机策略，最后通过 `custom` profile 展示选做题中的自定义优先级负载均衡策略。整个项目的核心目标是说明：服务消费者无需写死商品服务地址，只需要通过注册中心发现 `product-service`，并由客户端负载均衡组件自动选择具体服务实例。

### 4.2 RestTemplate 完成 RESTful 远程调用

本步骤使用 Spring 提供的 RestTemplate 完成服务消费者到服务提供者的 RESTful 远程调用。`cart-service` 中创建了带 `@LoadBalanced` 注解的 RestTemplate Bean，因此调用地址可以直接写成 `http://product-service/api/products/...`，不需要写死具体的 `8091` 或 `8093` 端口。请求发出后，Spring Cloud LoadBalancer 会先根据服务名 `product-service` 从 Nacos 中获取可用实例，再选择其中一个商品服务实例完成 HTTP 调用。

为了满足实验要求，商品服务提供了 GET、POST、PUT、DELETE 四种 HTTP 方法，购物车服务分别封装了对应的 RestTemplate 调用接口。测试时只需要访问购物车服务的 `/api/carts/rest/products` 相关接口，就可以间接验证服务发现、远程调用和 HTTP 方法映射是否正确。返回结果中的 `method` 字段可以证明调用的 HTTP 方法，`servedBy` 字段可以证明实际处理请求的是哪个商品服务实例。

代码位置：

- RestTemplate Bean：[RestTemplateConfig.java](cart-service/src/main/java/com/zxylearn/demo2/cart/config/RestTemplateConfig.java)
- RestTemplate 调用入口：[CartController.java](cart-service/src/main/java/com/zxylearn/demo2/cart/controller/CartController.java)
- 商品 RESTful 接口：[ProductController.java](product-service-a/src/main/java/com/zxylearn/demo2/product/controller/ProductController.java)

验证 GET：

```bash
curl http://localhost:8092/api/carts/rest/products/2001
```

验证 POST：

```bash
curl -X POST http://localhost:8092/api/carts/rest/products \
  -H "Content-Type: application/json" \
  -d '{"productId":"3001","name":"USB Cable","price":29.90}'
```

验证 PUT：

```bash
curl -X PUT http://localhost:8092/api/carts/rest/products/2001 \
  -H "Content-Type: application/json" \
  -d '{"name":"Mechanical Keyboard","price":259.00}'
```

验证 DELETE：

```bash
curl -X DELETE http://localhost:8092/api/carts/rest/products/2002
```

截图重点：

- 返回 JSON 中 `method` 分别为 `GET`、`POST`、`PUT`、`DELETE`
- `servedBy` 显示实际处理请求的商品服务实例，如 `product-service:8091(product-a)`
- `registry` 显示 `nacos`

### 4.3 OpenFeign 完成远程调用

本步骤使用 OpenFeign 完成声明式远程服务调用。与 RestTemplate 需要手动拼接 URL、构造请求不同，OpenFeign 通过 Java 接口描述远程服务的调用方式。`cart-service` 在主启动类上使用 `@EnableFeignClients` 开启 Feign 支持，并创建 `ProductFeignClient` 接口，将商品服务的 GET、POST、PUT、DELETE 接口映射为 Java 方法。

FeignClient 中的 `name = "product-service"` 表示调用目标不是固定 IP 和端口，而是注册中心中的服务名。运行时 OpenFeign 会结合 Nacos 服务发现和 Spring Cloud LoadBalancer 自动选择商品服务实例。这样购物车服务只需要像调用本地接口一样调用 `ProductFeignClient` 方法，就能完成跨服务 HTTP 调用，代码更简洁，也更符合微服务项目中接口契约化管理的方式。

代码位置：

- 主启动类启用 Feign：[CartServiceApplication.java](cart-service/src/main/java/com/zxylearn/demo2/cart/CartServiceApplication.java)
- FeignClient 接口：[ProductFeignClient.java](cart-service/src/main/java/com/zxylearn/demo2/cart/client/ProductFeignClient.java)
- Feign 调用入口：[CartController.java](cart-service/src/main/java/com/zxylearn/demo2/cart/controller/CartController.java)

验证 GET：

```bash
curl http://localhost:8092/api/carts/feign/products/2001
```

验证 POST：

```bash
curl -X POST http://localhost:8092/api/carts/feign/products \
  -H "Content-Type: application/json" \
  -d '{"productId":"3002","name":"Laptop Stand","price":89.00}'
```

验证 PUT：

```bash
curl -X PUT http://localhost:8092/api/carts/feign/products/2003 \
  -H "Content-Type: application/json" \
  -d '{"name":"Wide Monitor","price":1299.00}'
```

验证 DELETE：

```bash
curl -X DELETE http://localhost:8092/api/carts/feign/products/2003
```

报告可写：

> 本实验在购物车服务中使用 `@EnableFeignClients` 开启 OpenFeign 支持，并通过 `@FeignClient(name = "product-service", path = "/api/products")` 声明商品服务客户端。购物车服务调用 FeignClient 方法即可完成对商品服务的 GET、POST、PUT、DELETE 远程调用。

### 4.4 服务提供者集群

本步骤用于搭建多个服务提供者实例，验证服务消费者可以通过同一个服务名访问多个后端实例。工程中创建了 `product-service-a` 和 `product-service-b` 两个 Maven 子模块，它们的业务代码一致，但运行端口不同，分别使用 `8091` 和 `8093`。两个模块在配置文件中使用相同的 `spring.application.name=product-service`，因此注册到 Nacos 后会表现为同一个服务下的两个实例。

这种配置方式模拟了实际生产环境中的服务集群部署。消费者 `cart-service` 并不关心具体实例端口，只通过服务名 `product-service` 发起调用。Nacos 控制台中可以看到 `product-service` 下存在两个健康实例，说明服务提供者集群已经搭建完成，也为后续的轮询、随机和自定义负载均衡实验提供了基础。

两个商品服务模块使用相同的 `spring.application.name`：

```yaml
spring:
  application:
    name: product-service
```

区别在于端口和元数据：

- `product-service-a`：`server.port=8091`，`instance=product-a`，`priority=1`
- `product-service-b`：`server.port=8093`，`instance=product-b`，`priority=2`

截图内容：

- Nacos 控制台 `DEMO2` 分组服务列表
- `product-service` 服务详情中展示两个实例
- 两个实例端口分别为 `8091` 和 `8093`

### 4.5 默认轮询负载均衡

默认启动购物车服务时，不指定 profile，Spring Cloud LoadBalancer 使用轮询策略。

本步骤用于验证 Spring Cloud LoadBalancer 的默认客户端负载均衡行为。在两个商品服务实例都启动并注册成功后，购物车服务连续多次调用商品服务的 `/api/products/ping` 接口。由于默认策略是轮询，请求会按照顺序依次分配给 `product-service-a` 和 `product-service-b`，返回结果中的 `servedBy` 字段会在两个实例之间交替出现。

轮询策略的特点是实现简单、分配均匀，适合各服务实例性能接近的场景。本实验通过 `times` 参数控制连续调用次数，一次请求即可返回多次调用结果，便于截图展示负载均衡效果。RestTemplate 和 OpenFeign 都接入了同一个 Spring Cloud LoadBalancer 机制，因此两种调用方式都能观察到轮询效果。

验证 RestTemplate 轮询：

```bash
curl "http://localhost:8092/api/carts/loadbalancer/rest?times=8"
```

验证 OpenFeign 轮询：

```bash
curl "http://localhost:8092/api/carts/loadbalancer/feign?times=8"
```

截图重点：

- 返回数组中 `servedBy` 在 `product-a` 和 `product-b` 之间交替出现
- 端口在 `8091` 和 `8093` 之间轮换

报告可写：

> Spring Cloud LoadBalancer 默认使用轮询策略。购物车服务连续调用商品服务 `/api/products/ping` 接口时，请求会在两个商品服务实例之间依次分配，从返回结果中的端口和实例标识可以观察到负载均衡效果。

### 4.6 随机负载均衡策略

本步骤将默认轮询策略修改为随机策略，用于验证 Spring Cloud LoadBalancer 可以针对指定服务配置不同的负载均衡算法。工程中为 `product-service` 创建了 `ProductLoadBalancerConfiguration`，并在 `random` profile 下启用该配置。配置类中创建 `RandomLoadBalancer` Bean，使购物车服务在调用商品服务时不再按固定顺序选择实例，而是随机选择一个可用实例。

测试随机策略时，需要使用 `random` profile 重新启动 `cart-service`。连续访问负载均衡测试接口后，可以看到返回结果仍然会命中 `product-a` 和 `product-b`，但命中顺序不再稳定交替，而是呈现随机分布。该步骤说明 Spring Cloud LoadBalancer 的策略可以通过配置切换，服务调用代码本身不需要修改。

代码位置：

- 随机策略启用配置：[ProductLoadBalancerConfiguration.java](cart-service/src/main/java/com/zxylearn/demo2/cart/config/ProductLoadBalancerConfiguration.java)
- 随机策略 Bean：[RandomLoadBalancerConfiguration.java](cart-service/src/main/java/com/zxylearn/demo2/cart/config/RandomLoadBalancerConfiguration.java)

使用 `random` profile 启动购物车服务：

```bash
./mvnw -pl cart-service spring-boot:run -Dspring-boot.run.profiles=random
```

再次验证：

```bash
curl "http://localhost:8092/api/carts/loadbalancer/rest?times=12"
curl "http://localhost:8092/api/carts/loadbalancer/feign?times=12"
```

截图重点：

- 返回结果仍会命中 `product-a` 和 `product-b`
- 命中顺序不再固定交替，而是随机分布

报告可写：

> 本实验通过 `@LoadBalancerClient` 为 `product-service` 指定自定义 LoadBalancer 配置，在 `random` profile 下创建 `RandomLoadBalancer` Bean，从而将默认轮询策略切换为随机策略。连续请求结果中实例命中顺序不固定，说明随机负载均衡策略生效。

### 4.7 选做题：自定义优先级负载均衡策略

本步骤完成实验选做题：基于 Spring Cloud LoadBalancer 自定义一种新的负载均衡策略。工程中实现了 `PriorityServiceInstanceLoadBalancer`，该类读取服务实例元数据中的 `priority` 字段，并优先选择 `priority` 数值最小的实例。当前 `product-service-a` 的优先级为 `1`，`product-service-b` 的优先级为 `2`，因此启用自定义策略后，请求会优先命中 `product-service-a`。

该策略模拟了实际系统中“按实例权重或优先级选择服务”的需求。例如某些实例机器配置更高、网络更近、或者希望作为主实例承担更多流量，就可以通过元数据配置优先级。实验中通过 `custom` profile 启用自定义策略，并连续调用负载均衡测试接口。如果结果稳定命中 `product-a`，就说明自定义 LoadBalancer 已经接管了默认实例选择逻辑。

代码位置：

- 自定义策略启用配置：[CustomProductLoadBalancerConfiguration.java](cart-service/src/main/java/com/zxylearn/demo2/cart/config/CustomProductLoadBalancerConfiguration.java)
- 自定义策略 Bean：[PriorityLoadBalancerConfiguration.java](cart-service/src/main/java/com/zxylearn/demo2/cart/config/PriorityLoadBalancerConfiguration.java)
- 自定义策略实现：[PriorityServiceInstanceLoadBalancer.java](cart-service/src/main/java/com/zxylearn/demo2/cart/loadbalancer/PriorityServiceInstanceLoadBalancer.java)

策略说明：

> 本实验基于 Spring Cloud LoadBalancer 自定义了 `PriorityServiceInstanceLoadBalancer`。该策略读取服务实例元数据中的 `priority` 字段，并选择 `priority` 数值最小的实例。当前 `product-service-a` 的 `priority=1`，`product-service-b` 的 `priority=2`，因此启用自定义策略后，请求会优先命中 `product-service-a`。

使用 `custom` profile 启动购物车服务：

```bash
./mvnw -pl cart-service spring-boot:run -Dspring-boot.run.profiles=custom
```

验证自定义策略：

```bash
curl "http://localhost:8092/api/carts/loadbalancer/rest?times=8"
curl "http://localhost:8092/api/carts/loadbalancer/feign?times=8"
```

截图重点：

- 返回数组中的 `servedBy` 稳定显示 `product-service:8091(product-a)`
- Nacos 服务详情中可以看到两个实例的元数据分别包含 `priority=1` 和 `priority=2`
- 与默认轮询结果对比，自定义策略不是依次轮换，而是按优先级选择实例

报告可写：

> 为完成选做题，本实验自定义了基于元数据优先级的负载均衡策略。`PriorityServiceInstanceLoadBalancer` 实现 Spring Cloud LoadBalancer 的 `ReactorServiceInstanceLoadBalancer` 接口，在每次选择实例时读取所有可用实例的 `priority` 元数据，并优先选择优先级数值最小的实例。启动 `custom` profile 后，连续访问负载均衡测试接口，请求稳定命中 `product-a`，说明自定义负载均衡策略已经生效。

### 4.8 服务发现结果

本步骤用于展示服务消费者从注册中心获取服务列表和实例元数据的结果。`cart-service` 中通过 `DiscoveryClient` 提供了两个辅助接口：一个用于查看当前可发现的服务名列表，另一个用于查看指定服务的实例详情。通过访问这些接口，可以证明 `product-service` 已经成功注册到 Nacos，并且消费者能够发现该服务下的多个实例。

服务发现结果中需要重点观察实例端口和元数据。端口 `8091`、`8093` 分别对应两个商品服务实例；元数据中的 `module`、`business-service`、`registry`、`version` 用于说明服务实例的业务含义和注册来源；`priority` 字段则用于支持选做题中的自定义优先级负载均衡策略。该步骤可以作为服务注册、发现和元数据配置成功的截图依据。

验证命令：

```bash
curl http://localhost:8092/api/carts/services
curl http://localhost:8092/api/carts/discovery/product-service
```

截图重点：

- 服务列表中包含 `product-service`
- 服务实例中包含端口 `8091` 和 `8093`
- 元数据包含 `module=product-provider`、`business-service=product-service`、`registry=nacos`、`version=1.0.0`、`priority`

## 五、OpenFeign 与 RestTemplate 对比

相同点：

- 都可以完成 HTTP 远程调用。
- 都可以结合 Nacos 服务发现按服务名调用服务实例。
- 都可以接入 Spring Cloud LoadBalancer 实现客户端负载均衡。

不同点：

- RestTemplate 是命令式模板 API，需要手动拼接 URL、构造请求体和处理 HTTP 方法。
- OpenFeign 是声明式客户端 API，只需要定义 Java 接口和 Spring MVC 注解。
- RestTemplate 更直观，适合简单调用和低层控制；OpenFeign 更适合微服务之间大量接口调用。

OpenFeign 主要优势：

- 代码更简洁，接口定义就是远程调用契约。
- 与 Spring MVC 注解风格一致，学习成本低。
- 默认集成服务发现和负载均衡，按服务名调用更方便。
- 调用方业务代码不需要关心 URL 拼接细节，可维护性更好。

## 六、实验总结

本实验基于 Nacos 注册中心搭建了两个商品服务实例和一个购物车服务消费者。商品服务提供 GET、POST、PUT、DELETE 四类 RESTful 接口，购物车服务分别使用 RestTemplate 和 OpenFeign 完成远程调用。通过两个商品服务实例使用相同服务名注册到 Nacos，购物车服务可以基于服务名 `product-service` 发现可用实例，并由 Spring Cloud LoadBalancer 在客户端完成负载均衡。

实验中可以观察到，默认配置下 LoadBalancer 使用轮询策略，连续请求会依次命中 `product-a` 和 `product-b`；启用 `random` profile 后，购物车服务为商品服务创建 `RandomLoadBalancer`，连续请求的实例命中顺序变为随机；启用 `custom` profile 后，自定义的 `PriorityServiceInstanceLoadBalancer` 会根据实例元数据中的 `priority` 字段选择优先级更高的实例。通过对比 RestTemplate 和 OpenFeign 可以看出，OpenFeign 更适合微服务项目中的声明式远程调用，能减少重复样板代码，提高调用接口的清晰度和可维护性。

## 七、参考文档

- Spring Cloud OpenFeign 官方文档：`https://github.com/spring-cloud/spring-cloud-openfeign/blob/main/docs/modules/ROOT/pages/spring-cloud-openfeign.adoc`
- Spring Cloud LoadBalancer 官方文档：`https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/loadbalancer.html`
