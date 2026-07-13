# 软件服务工程实验四报告说明

本工程用于完成“微服务 API 网关 Gateway”实验。项目包含两个商品服务实例、一个购物车服务和一个 Spring Cloud Gateway 网关服务，所有服务统一注册到 Nacos，网关通过 `lb://` 按服务名转发请求，并实现全局认证、内部访问保护、跨域、负载均衡、熔断降级和简单流量管控。

GitHub 地址：`https://github.com/01-Dreamer/sp_homework`

## 一、工程模块

| 模块 | 端口 | 服务名 | 作用 |
| --- | ---: | --- | --- |
| `product-service-a` | 8201 | `demo4-product-service` | 商品服务实例 A |
| `product-service-b` | 8202 | `demo4-product-service` | 商品服务实例 B，用于展示负载均衡 |
| `cart-service` | 8203 | `demo4-cart-service` | 购物车服务 |
| `gateway-service` | 8200 | `demo4-gateway-service` | API 网关服务 |

Nacos 配置：

- 控制台：`http://SERVER_IP_PLACEHOLDER:8848/nacos`
- 用户名：`nacos`
- 密码：`20230607`
- 服务分组：`DEMO4`
- 客户端注册地址：`SERVER_IP_PLACEHOLDER:8848`

## 二、版本与依赖

父工程 [pom.xml](pom.xml) 统一管理版本：

```xml
<java.version>21</java.version>
<spring.boot.version>4.1.0</spring.boot.version>
<spring.cloud.version>2025.1.2</spring.cloud.version>
<spring.cloud.alibaba.version>2025.1.0.0</spring.cloud.alibaba.version>
```

服务提供者依赖：

- `spring-boot-starter-webmvc`
- `spring-cloud-starter-alibaba-nacos-discovery`
- `spring-boot-starter-actuator`

网关服务依赖：

- `spring-cloud-starter-gateway-server-webflux`
- `spring-cloud-starter-loadbalancer`
- `spring-cloud-starter-alibaba-nacos-discovery`
- `spring-cloud-starter-circuitbreaker-reactor-resilience4j`
- `spring-boot-starter-actuator`

## 三、启动命令

先编译：

```bash
cd /home/failedman/homework/demo4
./mvnw clean package -DskipTests
```

分别启动四个服务：

```bash
./mvnw -pl product-service-a spring-boot:run
./mvnw -pl product-service-b spring-boot:run
./mvnw -pl cart-service spring-boot:run
./mvnw -pl gateway-service spring-boot:run
```

如果需要临时指定 Nacos 地址：

```bash
NACOS_SERVER_ADDR=SERVER_IP_PLACEHOLDER:8848 NACOS_USERNAME=nacos NACOS_PASSWORD=20230607 \
./mvnw -pl gateway-service spring-boot:run
```

## 四、关键实验步骤

### 4.1 实验项目概述

本实验项目是一个基于 Spring Cloud Gateway 的 API 网关示例工程。商品服务和购物车服务作为后端微服务注册到 Nacos，网关服务也注册到 Nacos，并通过路由配置统一暴露后端接口。

本实验重点展示网关在微服务架构中的统一入口作用，包括服务路由、断言匹配、过滤器增强、服务发现负载均衡、统一认证、内部访问保护、跨域配置和容错保护。为了避免客户端绕过网关直接访问后端微服务，网关在转发请求时会自动添加内部令牌请求头 `X-Internal-Toekn`，商品服务和购物车服务会对该请求头进行校验；客户端不再直接访问 `8201`、`8202`、`8203`，而是统一访问 `8200` 的 Gateway。

### 4.2 网关服务创建与注册中心配置

`gateway-service` 引入 `spring-cloud-starter-gateway-server-webflux` 作为网关核心依赖，并引入 Nacos Discovery 将网关注册到服务注册中心。配置文件中 `spring.application.name=demo4-gateway-service`，Nacos 分组为 `DEMO4`。

代码位置：

- 网关启动类：[GatewayServiceApplication.java](gateway-service/src/main/java/com/zxylearn/demo4/gateway/GatewayServiceApplication.java)
- 网关配置：[application.yml](gateway-service/src/main/resources/application.yml)

验证方式：

```bash
curl http://localhost:8200/actuator/health
```

实验结果说明：启动四个服务后，Nacos 控制台的 `DEMO4` 分组中可以看到 `demo4-gateway-service`、`demo4-product-service` 和 `demo4-cart-service`。其中 `demo4-product-service` 有两个实例，分别对应 8201 和 8202。

### 4.3 Gateway 路由、断言与过滤器配置

网关配置了两个核心路由：

```yaml
- id: product-route
  uri: lb://demo4-product-service
  predicates:
    - Path=/api/products/**

- id: cart-route
  uri: lb://demo4-cart-service
  predicates:
    - Path=/api/carts/**
```

`id` 用于标识路由，`uri` 使用 `lb://` 表示交给 Spring Cloud LoadBalancer 根据服务名选择实例，`Path` 断言用于判断请求路径是否匹配该路由。路由中还配置了 `AddRequestHeader=X-Internal-Toekn` 和 `AddResponseHeader=X-Gateway-Route`，前者用于给后端微服务注入内部访问令牌，后者用于在响应头中展示当前命中的路由。

测试命令：

```bash
curl -i -H "X-Token: <TOKEN_PLACEHOLDER>" "http://localhost:8200/api/products/4001"
curl -i -H "X-Token: <TOKEN_PLACEHOLDER>" "http://localhost:8200/api/carts/C1001"
```

实验结果说明：商品请求会被转发到 `demo4-product-service`，购物车请求会被转发到 `demo4-cart-service`。响应头中的 `X-Gateway-Route` 可以证明请求经过了 Gateway 的指定路由。

### 4.4 Gateway 请求路由功能展示

商品服务直连地址为：

```bash
curl -i "http://localhost:8201/api/products/4001"
curl -i "http://localhost:8202/api/products/4001"
```

通过网关访问地址为：

```bash
curl -H "X-Token: <TOKEN_PLACEHOLDER>" "http://localhost:8200/api/products/4001"
```

实验结果说明：直连微服务时没有 `X-Internal-Toekn` 内部请求头，商品服务会返回 `403 Forbidden`，防止客户端绕过网关直接访问后端服务。通过 Gateway 访问时，网关会自动添加内部请求头，因此请求可以正常转发到后端微服务。

### 4.5 Gateway 负载均衡配置与效果

`product-service-a` 和 `product-service-b` 使用相同服务名 `demo4-product-service` 注册到 Nacos。Gateway 的商品路由使用：

```yaml
uri: lb://demo4-product-service
```

因此请求会经过 Spring Cloud LoadBalancer，在两个商品服务实例之间分发。

测试命令：

```bash
for i in {1..8}; do
  curl -s -H "X-Token: <TOKEN_PLACEHOLDER>" "http://localhost:8200/api/products/4001"
  echo
done
```

实验结果说明：多次请求返回的 `servedBy` 字段会在 `product-service-a:8201` 和 `product-service-b:8202` 之间变化，说明 Gateway 没有写死具体地址，而是通过 Nacos 服务发现和 LoadBalancer 完成了负载均衡。

### 4.6 全局认证过滤器

网关实现了全局认证过滤器 [AuthGlobalFilter.java](gateway-service/src/main/java/com/zxylearn/demo4/gateway/filter/AuthGlobalFilter.java)。该过滤器会拦截所有 `/api/**` 请求，并要求请求头中携带：

```text
X-Token: <TOKEN_PLACEHOLDER>
```

未携带令牌：

```bash
curl -i "http://localhost:8200/api/products/4001"
```

携带正确令牌：

```bash
curl -i -H "X-Token: <TOKEN_PLACEHOLDER>" "http://localhost:8200/api/products/4001"
```

实验结果说明：未携带令牌时，网关直接返回 `401 Unauthorized`，请求不会继续转发到后端服务；携带正确令牌时，请求可以正常路由到商品服务。该过滤器体现了 Gateway 作为统一入口进行权限校验的作用。

### 4.7 微服务内部访问保护

为了避免客户端绕过 Gateway 直接访问后端微服务，`product-service-a`、`product-service-b` 和 `cart-service` 都配置了 MVC 拦截器，统一检查 `/api/**` 请求是否携带内部请求头：

```text
X-Internal-Toekn: demo4-internal-token
```

代码位置：

- 商品服务拦截器：[InternalTokenInterceptor.java](product-service-a/src/main/java/com/zxylearn/demo4/product/config/InternalTokenInterceptor.java)
- 购物车服务拦截器：[InternalTokenInterceptor.java](cart-service/src/main/java/com/zxylearn/demo4/cart/config/InternalTokenInterceptor.java)
- 网关注入内部头：[application.yml](gateway-service/src/main/resources/application.yml)

直接访问微服务：

```bash
curl -i "http://localhost:8201/api/products/4001"
curl -i "http://localhost:8203/api/carts/C1001"
```

通过网关访问微服务：

```bash
curl -i -H "X-Token: <TOKEN_PLACEHOLDER>" "http://localhost:8200/api/products/4001"
curl -i -H "X-Token: <TOKEN_PLACEHOLDER>" "http://localhost:8200/api/carts/C1001"
```

实验结果说明：直接访问 8201 或 8203 时，请求缺少内部令牌，会被微服务自身拦截并返回 `403 Forbidden`。通过 8200 网关访问时，Gateway 在转发前自动添加 `X-Internal-Toekn`，后端微服务校验通过，说明后端服务只能被网关可信调用。

### 4.8 全局跨域策略配置

网关在 [application.yml](gateway-service/src/main/resources/application.yml) 中通过 `spring.cloud.gateway.server.webflux.globalcors` 配置了全局 CORS：

```yaml
globalcors:
  add-to-simple-url-handler-mapping: true
  cors-configurations:
    '[/**]':
      allowedOriginPatterns:
        - "*"
      allowedMethods:
        - GET
        - POST
        - PUT
        - DELETE
        - OPTIONS
      allowedHeaders:
        - "*"
      allowCredentials: false
      maxAge: 3600
```

配置含义：

- `allowedOriginPatterns: "*"` 表示允许任意来源访问。
- `allowedMethods` 表示允许的 HTTP 方法。
- `allowedHeaders: "*"` 表示允许客户端携带任意请求头。
- `allowCredentials: false` 表示不允许携带 Cookie 等凭证，避免与通配来源冲突。
- `maxAge: 3600` 表示预检请求结果缓存 3600 秒。

测试命令：

```bash
curl -i -X OPTIONS "http://localhost:8200/api/products/4001" \
  -H "Origin: http://example.com" \
  -H "Access-Control-Request-Method: GET"
```

实验结果说明：响应中会出现 `Access-Control-Allow-Origin`、`Access-Control-Allow-Methods` 等跨域响应头，说明 Gateway 的全局跨域策略已经生效。

### 4.9 Gateway 容错保护：熔断降级

网关商品路由和购物车路由都配置了 CircuitBreaker 过滤器。商品服务发生异常或返回 500 时，网关会进入 fallback，不再把错误直接暴露给客户端。

核心配置：

```yaml
filters:
  - name: CircuitBreaker
    args:
      name: productGatewayCircuitBreaker
      fallbackUri: forward:/fallback/products
      statusCodes: 500
```

Fallback 代码位置：

- [FallbackController.java](gateway-service/src/main/java/com/zxylearn/demo4/gateway/controller/FallbackController.java)

测试命令：

```bash
for i in {1..4}; do
  curl -s -H "X-Token: <TOKEN_PLACEHOLDER>" "http://localhost:8200/api/products/4001?fail=true"
  echo
done
```

实验结果说明：后端商品服务抛出异常后，Gateway 的熔断过滤器会将请求转发到 `/fallback/products`，返回包含 `fallback=true` 的 JSON。该机制可以避免后端服务异常时把原始错误直接返回给客户端。

### 4.10 Gateway 流量管控：全局限流过滤器

网关实现了简单的全局限流过滤器 [SimpleRateLimitGlobalFilter.java](gateway-service/src/main/java/com/zxylearn/demo4/gateway/filter/SimpleRateLimitGlobalFilter.java)。当前配置为每个客户端 5 秒内最多允许 8 个 `/api/**` 请求通过，超过阈值后返回 `429 Too Many Requests`。

核心配置：

```yaml
demo4:
  gateway:
    rate-limit:
      max-requests: 8
      window: 5s
```

测试命令：

```bash
seq 1 12 | xargs -I{} -P12 curl -s -H "X-Token: <TOKEN_PLACEHOLDER>" \
  "http://localhost:8200/api/products/4001"
```

实验结果说明：前 8 个左右请求可以正常转发到后端商品服务，超过 5 秒窗口阈值的请求会被网关直接拒绝，返回 `rateLimited=true`。这说明流量管控发生在 Gateway 层，后端服务不会承受超过阈值的突发请求。

### 4.11 并发测试建议

如果使用 k6 进行压测，可以直接使用项目中的 [demo4-gateway-k6.js](demo4-gateway-k6.js)：

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
    http_req_duration: ['p(95)<3000'],
  },
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8200';
const headers = { 'X-Token': __ENV.TOKEN || 'API_KEY_PLACEHOLDER' };

export default function () {
  const res = http.get(`${baseUrl}/api/products/4001`, { headers });
  check(res, {
    'status is 200 or 429': (r) => r.status === 200 || r.status === 429,
    'gateway response': (r) => r.body.length > 0,
  });
  sleep(1);
}
```

运行命令：

```bash
k6 run demo4-gateway-k6.js
```

测试结果分析：k6 并发请求会持续经过 Gateway 的认证、路由、负载均衡和限流过滤器。若并发速率超过 5 秒 8 次的限制，部分请求会返回 429；未被限流的请求会转发到两个商品服务实例，体现负载均衡效果。

## 五、实验总结

本实验通过 Spring Cloud Gateway 搭建了微服务统一入口，并将 `gateway-service`、两个商品服务实例和购物车服务全部注册到 Nacos。网关服务使用 `Path` 断言区分商品接口和购物车接口，分别将 `/api/products/**` 和 `/api/carts/**` 转发到对应的后端服务；同时使用 `AddResponseHeader` 在响应中标记命中的路由，方便观察 Gateway 的路由匹配结果。

在服务发现和负载均衡方面，两个商品服务实例使用相同的服务名 `demo4-product-service` 注册到 Nacos，Gateway 通过 `lb://demo4-product-service` 按服务名转发请求。多次访问商品接口时，返回结果中的 `servedBy` 会在 `product-service-a:8201` 和 `product-service-b:8202` 之间切换，说明网关没有写死实例地址，而是通过 Nacos 和 Spring Cloud LoadBalancer 完成动态路由与负载均衡。

在安全控制方面，本实验实现了两层保护。第一层是 Gateway 全局认证过滤器，客户端访问 `/api/**` 时必须携带 `X-Token: <TOKEN_PLACEHOLDER> `401 Unauthorized`；第二层是微服务内部令牌保护，网关转发请求时自动添加 `X-Internal-Toekn`，商品服务和购物车服务会校验该请求头，直接访问 `8201`、`8202`、`8203` 时会因为缺少内部令牌而返回 `403 Forbidden`。这样既能验证网关统一认证的作用，也能防止客户端绕过网关直接访问后端微服务。

在跨域和容错方面，Gateway 配置了全局 CORS 策略，统一允许常见 HTTP 方法和请求头，并通过预检请求验证了跨域响应头可以正常返回。网关还配置了 CircuitBreaker 过滤器，当商品服务模拟异常返回 500 时，请求会被转发到 fallback 接口，返回包含 `fallback=true` 的降级结果，避免后端异常直接暴露给客户端。

在流量治理方面，本实验实现了一个全局限流过滤器，限制同一客户端 5 秒内最多访问 8 次 `/api/**`。并发测试中，超过阈值的请求会被网关直接拒绝并返回 `429 Too Many Requests`，说明限流发生在网关层，后端微服务不会承受超过阈值的突发流量。k6 压测脚本也验证了在并发请求下 Gateway 可以同时完成认证、路由、负载均衡和限流处理。

通过本实验可以看出，Gateway 不只是简单的请求转发组件，而是微服务系统中的统一治理入口。它可以把认证、跨域、路由、负载均衡、限流和熔断等公共能力集中在网关层完成，减少每个业务微服务重复实现这些逻辑的成本，也让客户端不需要感知后端服务实例数量、端口和部署变化，从而提升系统的可维护性和稳定性。

## 六、参考文档

- Spring Cloud Gateway 官方文档：`https://docs.spring.io/spring-cloud-gateway/reference/`
- Spring Cloud LoadBalancer 官方文档：`https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/loadbalancer.html`
- Spring Cloud Alibaba Nacos Discovery：`https://sca.aliyun.com/docs/2023/user-guide/nacos/quick-start/`
