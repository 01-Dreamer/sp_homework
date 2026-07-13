# 软件服务工程实验三报告说明

本工程用于完成“微服务熔断、隔离、限流与服务降级”实验。工程同时实现 Resilience4j 和 Sentinel 两套容错方案，注册中心统一使用 Nacos。

GitHub 地址：`https://github.com/01-Dreamer/sp_homework`

## 一、工程模块

| 模块 | 端口 | 作用 | 注册到 Nacos 的服务名 |
| --- | ---: | --- | --- |
| `product-service` | 8111 | 商品服务提供者，用于模拟成功、失败和延迟响应 | `product-service` |
| `inventory-service` | 8112 | 库存服务提供者，用于模拟成功、失败和慢调用 | `inventory-service` |
| `resilience-order-service` | 8113 | Resilience4j 版订单服务消费者 | `resilience-order-service` |
| `sentinel-order-service` | 8114 | Sentinel 版订单服务消费者 | `sentinel-order-service` |

Nacos 配置：

- 控制台：`http://SERVER_IP_PLACEHOLDER:8848/nacos`
- 用户名：`nacos`
- 密码：`20230607`
- 服务分组：`DEMO3`
- 客户端注册地址：`SERVER_IP_PLACEHOLDER:8848`

## 二、版本与依赖

父工程 [pom.xml](pom.xml) 使用 `dependencyManagement` 统一管理版本：

```xml
<java.version>21</java.version>
<spring.boot.version>4.1.0</spring.boot.version>
<spring.cloud.version>2025.1.2</spring.cloud.version>
<spring.cloud.alibaba.version>2025.1.0.0</spring.cloud.alibaba.version>
<resilience4j.version>2.3.0</resilience4j.version>
```

消费者公共依赖：

- `spring-cloud-starter-openfeign`
- `spring-cloud-starter-loadbalancer`
- `spring-cloud-starter-alibaba-nacos-discovery`

Resilience4j 版本消费者额外引入：

- `resilience4j-spring-boot3`
- `spring-aop`
- `aspectjweaver`
- `spring-boot-starter-actuator`
- `spring-boot-starter-cache`
- `caffeine`

Sentinel 版本消费者额外引入：

- `spring-cloud-starter-alibaba-sentinel`
- `sentinel-parameter-flow-control`

## 三、启动命令

先编译：

```bash
./mvnw clean package -DskipTests
```

启动服务提供者：

```bash
./mvnw -pl product-service spring-boot:run
./mvnw -pl inventory-service spring-boot:run
```

启动 Resilience4j 版消费者：

```bash
./mvnw -pl resilience-order-service spring-boot:run
```

启动 Sentinel 版消费者：

```bash
./mvnw -pl sentinel-order-service spring-boot:run
```

如果需要临时指定 Nacos 地址：

```bash
NACOS_SERVER_ADDR=SERVER_IP_PLACEHOLDER:8848 NACOS_USERNAME=nacos NACOS_PASSWORD=20230607 \
./mvnw -pl resilience-order-service spring-boot:run
```

## 四、关键实验步骤

### 4.1 实验项目概述

本实验项目是一个基于 Spring Cloud 的微服务容错示例工程。项目中包含两个服务提供者：`product-service` 和 `inventory-service`，它们都支持通过请求参数模拟正常响应、失败响应和延迟响应；同时包含两个服务消费者：`resilience-order-service` 使用 Resilience4j 实现容错保护，`sentinel-order-service` 使用 Sentinel 实现容错保护。两个消费者都通过 OpenFeign 按服务名调用远程服务，并通过 Nacos 完成服务注册与发现。

本实验的核心目标是观察微服务调用链中出现失败、慢调用或高并发访问时，消费者如何通过熔断、隔离、限流和服务降级保护自身。为了便于对比，Resilience4j 和 Sentinel 两套实现使用相同的远程服务、相近的测试接口和类似的降级响应结构，返回 JSON 中的 `fallback=true` 表示容错机制已经触发。

### 4.2 服务提供者的故障模拟接口

商品服务和库存服务都提供用于实验的远程接口。接口支持两个关键参数：`fail` 用于模拟远程服务返回异常，`delayMillis` 用于模拟远程服务慢响应。这样消费者侧可以通过不同参数制造失败调用和慢调用，从而触发断路器、隔离器和限流器。

商品服务代码位置：

- [ProductController.java](product-service/src/main/java/com/zxylearn/demo3/product/controller/ProductController.java)

库存服务代码位置：

- [InventoryController.java](inventory-service/src/main/java/com/zxylearn/demo3/inventory/controller/InventoryController.java)

直连验证：

```bash
curl "http://localhost:8111/api/products/3001"
curl "http://localhost:8111/api/products/3001?fail=true"
curl "http://localhost:8112/api/inventories/3001?delayMillis=2500"
```

截图重点：

- 正常请求返回商品或库存 JSON。
- `fail=true` 时返回服务端错误，用于触发失败率熔断。
- `delayMillis=2500` 时响应超过 2 秒，用于触发慢调用熔断。

### 4.3 Resilience4j 断路器实例 A：失败率熔断

本步骤配置 Resilience4j 断路器实例 `circuitBreakerA`，用于保护订单服务对商品服务的 OpenFeign 调用。该断路器使用时间窗口统计最近 10 秒内的请求结果，最小请求数为 5，当失败率达到 30% 时进入 OPEN 状态。进入 OPEN 状态后，后续请求不会继续访问商品服务，而是直接进入 fallback 降级响应；等待 5 秒后自动进入 HALF_OPEN 状态，并允许 3 个请求测试服务是否恢复。

代码位置：

- Resilience4j 配置：[application.yml](resilience-order-service/src/main/resources/application.yml)
- Feign 调用：[ProductClient.java](resilience-order-service/src/main/java/com/zxylearn/demo3/order/client/ProductClient.java)
- 保护入口：[OrderController.java](resilience-order-service/src/main/java/com/zxylearn/demo3/order/controller/OrderController.java)

触发失败率熔断：

```bash
for i in {1..5}; do
  curl "http://localhost:8113/api/orders/circuit-a/products/3001?fail=true"
  echo
done
```

查看状态：

```bash
curl http://localhost:8113/api/orders/resilience/status
```

截图重点：

- 多次失败后返回中出现 `fallback=true`。
- `circuitBreakerA.state` 变为 `OPEN`。
- 继续请求时不再真正调用商品服务，而是直接降级。

### 4.4 Resilience4j 断路器实例 B：慢调用熔断

本步骤配置 Resilience4j 断路器实例 `circuitBreakerB`，用于保护订单服务对库存服务的 OpenFeign 调用。该断路器除失败率阈值 50% 外，还配置了慢调用比例阈值 30%，慢调用判断阈值为 2 秒。也就是说，如果统计窗口内请求数量达到 5 个，并且超过 30% 的请求耗时超过 2 秒，则断路器会进入 OPEN 状态。

触发慢调用熔断：

```bash
seq 1 5 | xargs -I{} -P5 curl -s \
  "http://localhost:8113/api/orders/circuit-b/inventories/3001?delayMillis=2500"
```

查看状态：

```bash
curl http://localhost:8113/api/orders/resilience/status
```

这里必须使用并发请求。因为断路器 B 使用 10 秒时间窗口，并且每个慢调用会延迟 2.5 秒；如果用 `for` 循环串行执行 5 次，总耗时会超过 10 秒，状态接口查看时窗口内可能凑不够 5 次调用，`slowCallRate` 会显示 `-1`，断路器也不会进入 `OPEN`。

报告可写：

> 失败率熔断关注调用是否抛出异常，适合远程服务直接报错、连接失败、HTTP 500 等场景；慢调用熔断关注响应时间是否超过阈值，适合服务虽然能返回结果但响应过慢的场景。慢调用如果不被治理，会持续占用调用线程和连接资源，最终引发调用方堆积甚至雪崩。

### 4.5 Resilience4j 隔离器：并发执行数量限制

本步骤配置 Resilience4j Bulkhead 隔离器 `inventoryBulkhead`，用于限制访问库存服务的并发执行数量。配置中允许最大并发调用数为 10，最大等待时间为 20ms。当并发请求超过隔离器容量时，超出的请求不会继续排队占用资源，而是快速进入 fallback，避免远程服务慢响应拖垮订单服务。

配置位置：

- [application.yml](resilience-order-service/src/main/resources/application.yml)

并发测试命令：

```bash
seq 1 20 | xargs -I{} -P20 curl -s \
  "http://localhost:8113/api/orders/bulkhead/inventories/3001?delayMillis=1000"
```

本测试对库存远程调用进行了并发数量限制：同一时刻最多允许 10 个请求进入库存服务调用，超过 10 个并发请求后最多只等待 20ms。由于测试命令一次性并发发出 20 个请求，并且库存接口人为延迟 1000ms，所以前 10 个左右请求会占用隔离器容量，后续请求会因为没有可用并发许可而触发 `inventoryBulkheadFallback` 降级。

截图重点：

- 部分请求返回 `fallback=false`，表示正常调用库存服务。
- 部分请求返回 `fallback=true`，表示被隔离器拒绝。
- `/api/orders/resilience/status` 中 `inventoryBulkhead.availableConcurrentCalls` 会随并发变化。

报告可写：

> 隔离器的作用是限制某个远程调用最多占用多少并发资源。当库存服务响应变慢时，隔离器可以防止大量请求堆积在库存调用上，从而保护订单服务中其他接口不被拖垮。

### 4.6 Resilience4j 限流器：2 秒最多 5 个请求

本步骤配置 Resilience4j RateLimiter 限流器 `productRateLimiter`，用于控制订单服务访问商品服务的请求速率。配置中刷新周期为 2 秒，每个刷新周期最多允许 5 个请求通过，超出部分不等待，直接进入 fallback。该配置可以模拟接口级流量治理，防止突发流量瞬间打满远程服务。

并发测试命令：

```bash
seq 1 10 | xargs -I{} -P10 curl -s \
  "http://localhost:8113/api/orders/rate-limit/products/3001"
```

本测试对商品远程调用进行了请求速率限制：每 2 秒最多允许 5 个请求获得执行许可，`timeoutDuration=0` 表示拿不到许可时不排队等待。测试命令在短时间内并发发出 10 个请求，因此只有约 5 个请求可以真正调用商品服务，其余请求会直接进入 `productRateLimiterFallback`，返回 `fallback=true`。

截图重点：

- 2 秒内前 5 个左右请求正常返回。
- 超出限额的请求返回 `fallback=true`。
- `/api/orders/resilience/status` 中 `productRateLimiter.availablePermissions` 会体现可用令牌数量。

报告可写：

> 限流器用于控制单位时间内允许进入某个资源的请求数量。它与隔离器不同，隔离器限制并发占用数量，限流器限制请求进入速率。限流可以防止突发高流量冲击后端服务，是流量治理中的常用措施。

### 4.7 Sentinel 容错保护实现

本步骤使用 Sentinel 实现与 Resilience4j 类似的容错保护。`sentinel-order-service` 通过 `@SentinelResource` 标记需要保护的远程调用资源，并在启动时通过代码注册降级规则和流控规则。Sentinel 版本同样包含失败比例熔断、慢调用比例熔断、线程数隔离和 QPS 限流。

代码位置：

- Sentinel 规则配置：[SentinelRuleConfig.java](sentinel-order-service/src/main/java/com/zxylearn/demo3/sentinel/config/SentinelRuleConfig.java)
- Sentinel 调用入口：[SentinelOrderController.java](sentinel-order-service/src/main/java/com/zxylearn/demo3/sentinel/controller/SentinelOrderController.java)

查看 Sentinel 规则：

```bash
curl http://localhost:8114/api/sentinel-orders/sentinel/rules
```

失败比例熔断：

```bash
for i in {1..5}; do
  curl "http://localhost:8114/api/sentinel-orders/circuit-a/products/3001?fail=true"
  echo
done
```

慢调用熔断：

```bash
seq 1 5 | xargs -I{} -P5 curl -s \
  "http://localhost:8114/api/sentinel-orders/circuit-b/inventories/3001?delayMillis=2500"
```

线程数隔离：

```bash
seq 1 20 | xargs -I{} -P20 curl -s \
  "http://localhost:8114/api/sentinel-orders/bulkhead/inventories/3001?delayMillis=1000"
```

本测试使用 Sentinel 线程数流控规则限制库存远程调用资源，规则中 `grade=THREAD`、阈值为 10，表示同一时间最多允许 10 个线程进入该资源。测试时库存接口延迟 1000ms，20 个并发请求会同时争抢资源，超过线程数阈值的请求会被 Sentinel 阻止，并进入 `blockHandler` 返回降级结果。

QPS 限流：

```bash
seq 1 10 | xargs -I{} -P10 curl -s \
  "http://localhost:8114/api/sentinel-orders/rate-limit/products/3001"
```

本测试使用 Sentinel QPS 流控规则限制商品远程调用资源，规则中 `grade=QPS`、阈值为 2.5，等价于平均每 2 秒最多通过约 5 个请求。测试命令短时间并发发出 10 个请求，请求速率明显超过规则阈值，因此超出部分会被 Sentinel 拦截，并返回 `fallback=true`。

报告可写：

> Sentinel 通过资源名识别需要保护的调用，并基于规则管理器加载降级和流控规则。与 Resilience4j 相比，Sentinel 更强调资源维度的实时流控和运行时规则治理，也可以配合 Sentinel Dashboard 观察资源访问情况。

### 4.8 选做题：热点参数限流、结果缓存与超时处理

实验文档中的选做题要求自行完成其他类型的容错配置与实现。本项目补充实现了三类能力：Sentinel 热点参数限流、Spring Cache + Caffeine 结果缓存、Resilience4j TimeLimiter 超时处理。这样除了基础的熔断、隔离和限流外，还可以展示针对热点商品、重复查询和慢响应超时的容错治理方式。

#### 4.8.1 Sentinel 热点参数限流

热点参数限流用于处理“某个参数值被频繁访问”的场景。本项目在 `sentinel-order-service` 中配置资源 `sentinelHotspotProduct`，按第 0 个参数 `productId` 进行统计。普通商品参数默认 QPS 阈值为 2，商品 `3001` 被设置为特殊热点参数，QPS 阈值为 1。因此，当短时间内连续访问 `/hotspot/products/3001` 时，会比访问其他商品更容易触发限流。

代码位置：

- 热点参数规则：[SentinelRuleConfig.java](sentinel-order-service/src/main/java/com/zxylearn/demo3/sentinel/config/SentinelRuleConfig.java)
- 热点参数接口：[SentinelOrderController.java](sentinel-order-service/src/main/java/com/zxylearn/demo3/sentinel/controller/SentinelOrderController.java)

测试命令：

```bash
seq 1 8 | xargs -I{} -P8 curl -s \
  "http://localhost:8114/api/sentinel-orders/hotspot/products/3001"
```

本测试限制的是同一个热点参数值的访问频率，而不是整个接口的总 QPS。也就是说，`productId=3001` 因为被配置为热点参数，只允许更低的通过速率；当并发请求都集中访问 3001 时，Sentinel 会认为这个参数值过热，并拦截超出阈值的请求。返回结果中出现 `fallback=true`，说明热点参数限流已经生效。

#### 4.8.2 结果缓存

结果缓存用于处理远程服务结果变化不频繁、重复查询较多的场景。本项目在 `resilience-order-service` 中使用 Spring Cache 和 Caffeine 配置 `productResultCache`，缓存 key 为 `productId`，缓存有效期为 60 秒。第一次查询商品时会调用远程 `product-service`，后续相同 `productId` 的请求会直接返回缓存结果，从而减少远程调用次数。

代码位置：

- 缓存服务：[ProductCacheService.java](resilience-order-service/src/main/java/com/zxylearn/demo3/order/service/ProductCacheService.java)
- 缓存配置：[application.yml](resilience-order-service/src/main/resources/application.yml)

测试命令：

```bash
curl -X DELETE "http://localhost:8113/api/orders/cache/products/3001"
curl "http://localhost:8113/api/orders/cache/products/3001?delayMillis=1500"
curl "http://localhost:8113/api/orders/cache/products/3001?fail=true"
```

本测试限制的是重复远程查询对下游商品服务的压力。第一次请求使用 `delayMillis=1500`，可以观察到返回结果中 `delayMillis` 为 1500；第二次请求虽然传入 `fail=true`，但因为同一个 `productId` 已经存在缓存，订单服务不会再调用失败的远程接口，而是直接返回缓存中的商品结果。报告截图时可以对比两次响应中的 `checkedAt` 是否相同，若相同说明第二次结果来自缓存。

#### 4.8.3 超时处理

超时处理用于处理远程服务长时间不响应的场景。本项目在 `resilience-order-service` 中配置 Resilience4j TimeLimiter 实例 `inventoryTimeout`，超时时间为 1 秒。库存接口默认延迟参数设置为 2000ms，因此访问超时接口时会触发超时降级，而不是一直等待库存服务返回。

代码位置：

- 超时配置：[application.yml](resilience-order-service/src/main/resources/application.yml)
- 超时接口：[OrderController.java](resilience-order-service/src/main/java/com/zxylearn/demo3/order/controller/OrderController.java)

测试命令：

```bash
curl "http://localhost:8113/api/orders/timeout/inventories/3001?delayMillis=2000"
curl "http://localhost:8113/api/orders/timeout/inventories/3001?delayMillis=200"
```

本测试限制的是远程库存调用的最长等待时间。第一个请求让库存服务延迟 2000ms，超过 `inventoryTimeout` 的 1 秒阈值，所以订单服务会快速进入 `inventoryTimeoutFallback` 并返回 `fallback=true`；第二个请求只延迟 200ms，未超过超时阈值，因此可以正常返回库存结果。该配置可以防止慢接口长时间占用调用线程。

### 4.9 k6 压力测试步骤

本实验使用 k6 进行并发压测。k6 的测试脚本使用 JavaScript 编写，通过 `options.stages` 设置并发用户数变化，通过 `thresholds` 设置简单的通过条件。执行压测前需要先启动四个服务，并确认 Nacos 控制台中 `product-service`、`inventory-service`、`resilience-order-service`、`sentinel-order-service` 都已经注册到 `DEMO3` 分组。

检查 k6 是否可用：

```bash
k6 version
```

#### 4.9.1 Resilience4j 压测脚本

测试前配置说明：本测试需要先启动 `product-service`、`inventory-service` 和 `resilience-order-service`，并确认它们已经注册到 Nacos。脚本会同时访问失败率熔断、慢调用熔断、隔离器、限流器和超时处理接口，用于集中观察 Resilience4j 的容错效果。

在任意目录创建 `demo3-resilience-k6.js`：

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '5s', target: 5 },
    { duration: '10s', target: 20 },
    { duration: '5s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.50'],
    http_req_duration: ['p(95)<4000'],
  },
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8113';

export default function () {
  const requests = [
    ['GET', `${baseUrl}/api/orders/circuit-a/products/3001?fail=true`],
    ['GET', `${baseUrl}/api/orders/circuit-b/inventories/3001?delayMillis=2500`],
    ['GET', `${baseUrl}/api/orders/bulkhead/inventories/3001?delayMillis=1000`],
    ['GET', `${baseUrl}/api/orders/rate-limit/products/3001`],
    ['GET', `${baseUrl}/api/orders/timeout/inventories/3001?delayMillis=2000`],
  ];

  const responses = http.batch(requests);

  responses.forEach((res) => {
    check(res, {
      'status is 200 or fallback response': (r) => r.status === 200,
      'response has fallback field': (r) => r.body && r.body.includes('fallback'),
    });
  });

  sleep(1);
}
```

执行压测：

```bash
k6 run demo3-resilience-k6.js
```

压测后查看 Resilience4j 状态：

```bash
curl http://localhost:8113/api/orders/resilience/status
```

测试结果分析：压测过程中，部分请求会因为远程失败、慢调用、并发超限、速率超限或超时而返回 `fallback=true`。状态接口中如果看到断路器进入 `OPEN`、限流器可用许可下降或隔离器并发许可变化，说明 Resilience4j 的保护规则已经生效。

报告中可以截图 k6 控制台结果、接口返回中的 `fallback=true`、以及状态接口中断路器状态从 `CLOSED` 变为 `OPEN` 的结果。

#### 4.9.2 Sentinel 压测脚本

测试前配置说明：本测试需要先启动 `product-service`、`inventory-service` 和 `sentinel-order-service`，并确认 Sentinel 规则接口可以返回降级规则、流控规则和热点参数规则。脚本会同时访问 Sentinel 的失败比例熔断、慢调用熔断、线程数隔离、QPS 限流和热点参数限流接口。

在任意目录创建 `demo3-sentinel-k6.js`：

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '5s', target: 5 },
    { duration: '10s', target: 20 },
    { duration: '5s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.50'],
    http_req_duration: ['p(95)<4000'],
  },
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8114';

export default function () {
  const requests = [
    ['GET', `${baseUrl}/api/sentinel-orders/circuit-a/products/3001?fail=true`],
    ['GET', `${baseUrl}/api/sentinel-orders/circuit-b/inventories/3001?delayMillis=2500`],
    ['GET', `${baseUrl}/api/sentinel-orders/bulkhead/inventories/3001?delayMillis=1000`],
    ['GET', `${baseUrl}/api/sentinel-orders/rate-limit/products/3001`],
    ['GET', `${baseUrl}/api/sentinel-orders/hotspot/products/3001`],
  ];

  const responses = http.batch(requests);

  responses.forEach((res) => {
    check(res, {
      'status is 200 or fallback response': (r) => r.status === 200,
      'response has fallback field': (r) => r.body && r.body.includes('fallback'),
    });
  });

  sleep(1);
}
```

执行压测：

```bash
k6 run demo3-sentinel-k6.js
```

查看 Sentinel 规则：

```bash
curl http://localhost:8114/api/sentinel-orders/sentinel/rules
```

测试结果分析：压测过程中，当请求触发 Sentinel 的降级规则、线程数规则、QPS 规则或热点参数规则时，接口会进入 `blockHandler` 并返回 `fallback=true`。如果规则接口中能看到 `degradeRules`、`flowRules` 和 `paramFlowRules`，并且压测结果中出现降级响应，说明 Sentinel 规则加载和拦截都正常。

报告中可以截图 k6 控制台结果、接口返回中的 `fallback=true`、以及 Sentinel 规则接口返回的熔断规则和流控规则。

#### 4.9.3 分项压测命令

测试前配置说明：分项压测用于单独观察某一种容错能力，避免全量脚本同时触发多个规则导致截图不清楚。执行前需要根据测试目标选择 `URL`、`VUS` 和 `DURATION`，例如慢调用熔断要保证 10 秒窗口内有足够请求，隔离器测试要让并发数超过 10。

如果希望分别观察某一种容错能力，推荐使用单接口脚本。这样每次压测只访问一个接口，截图更清楚。

创建 `demo3-single-k6.js`：

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: Number(__ENV.VUS || 10),
  duration: __ENV.DURATION || '10s',
  thresholds: {
    http_req_duration: ['p(95)<4000'],
  },
};

const url = __ENV.URL;

export default function () {
  const res = http.get(url);
  check(res, {
    'status is 200': (r) => r.status === 200,
    'response has fallback field': (r) => r.body && r.body.includes('fallback'),
  });
  sleep(1);
}
```

Resilience4j 分项压测：

```bash
URL="http://localhost:8113/api/orders/circuit-a/products/3001?fail=true" \
VUS=5 DURATION=10s k6 run demo3-single-k6.js

URL="http://localhost:8113/api/orders/circuit-b/inventories/3001?delayMillis=2500" \
VUS=8 DURATION=10s k6 run demo3-single-k6.js

URL="http://localhost:8113/api/orders/bulkhead/inventories/3001?delayMillis=1000" \
VUS=20 DURATION=10s k6 run demo3-single-k6.js

URL="http://localhost:8113/api/orders/rate-limit/products/3001" \
VUS=10 DURATION=10s k6 run demo3-single-k6.js

URL="http://localhost:8113/api/orders/timeout/inventories/3001?delayMillis=2000" \
VUS=10 DURATION=10s k6 run demo3-single-k6.js
```

Sentinel 分项压测：

```bash
URL="http://localhost:8114/api/sentinel-orders/circuit-a/products/3001?fail=true" \
VUS=5 DURATION=10s k6 run demo3-single-k6.js

URL="http://localhost:8114/api/sentinel-orders/circuit-b/inventories/3001?delayMillis=2500" \
VUS=8 DURATION=10s k6 run demo3-single-k6.js

URL="http://localhost:8114/api/sentinel-orders/bulkhead/inventories/3001?delayMillis=1000" \
VUS=20 DURATION=10s k6 run demo3-single-k6.js

URL="http://localhost:8114/api/sentinel-orders/rate-limit/products/3001" \
VUS=10 DURATION=10s k6 run demo3-single-k6.js

URL="http://localhost:8114/api/sentinel-orders/hotspot/products/3001" \
VUS=8 DURATION=10s k6 run demo3-single-k6.js
```

测试结果分析：分项压测时，每次只观察一个接口的返回结果和对应状态，看到 `fallback=true` 就说明该接口被对应的容错规则保护并触发了降级。失败率熔断重点看断路器状态，慢调用熔断重点看慢调用比例，隔离和限流重点看并发或速率超出阈值后的拒绝结果，热点参数限流重点看同一个 `productId` 被集中访问时的拦截结果。

压测观察重点：

- 失败率熔断：多次 `fail=true` 后，请求快速返回 `fallback=true`。
- 慢调用熔断：`delayMillis=2500` 达到慢调用比例阈值后，后续请求进入降级。
- 隔离器：并发访问慢库存接口时，超出并发容量的请求进入 fallback。
- 限流器：短时间大量请求商品接口时，超过允许速率的请求进入 fallback。
- 热点参数限流：请求集中访问商品 `3001` 时，超过该参数单独阈值的请求进入 fallback。
- 超时处理：库存调用超过 1 秒时进入 fallback，短延迟请求正常返回。
- Nacos 控制台：四个服务都在 `DEMO3` 分组下正常注册。

## 五、OpenFeign 调用保护说明

本实验中，消费者没有直接写死服务地址，而是通过 OpenFeign 按服务名调用远程服务：

- `product-service`
- `inventory-service`

Resilience4j 和 Sentinel 的注解都加在消费者业务入口上，用于保护 OpenFeign 远程调用。当远程服务失败、慢调用、并发过高或请求过快时，消费者会返回本地 fallback 响应，从而避免异常继续向上扩散。

## 六、实验总结

本实验通过 Resilience4j 和 Sentinel 两套方案实现了微服务容错保护。Resilience4j 版本通过配置断路器、隔离器和限流器实例，并使用注解绑定到 OpenFeign 调用入口，完成失败率熔断、慢调用熔断、并发隔离和限流降级；Sentinel 版本通过资源名和规则管理器配置降级规则、线程数流控规则和 QPS 流控规则，实现了类似的保护能力。

通过测试可以看到，失败率熔断适合远程服务直接异常的场景，慢调用熔断适合远程服务响应过慢但未报错的场景，隔离器可以限制慢调用占用的并发资源，限流器可以限制单位时间内进入服务的请求数量。它们共同解决的是微服务系统中的雪崩问题：当某个下游服务异常时，调用方不会无限等待或持续重试，而是快速失败并返回降级结果，从而保护整个系统的稳定性。

## 七、参考文档

- Resilience4j 官方文档：`https://github.com/resilience4j/resilience4j`
- Spring Cloud CircuitBreaker 官方文档：`https://github.com/spring-cloud/spring-cloud-circuitbreaker`
- Sentinel 官方文档：`https://sentinelguard.io/`
- k6 官方文档：`https://grafana.com/docs/k6/latest/`
