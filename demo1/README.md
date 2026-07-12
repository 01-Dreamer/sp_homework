# 软件服务工程实验一报告说明

本工程用于完成“服务注册中心、服务注册与发现、远程服务调用”实验。工程同时实现 Eureka 和 Nacos 两套方案，业务服务统一为商品服务 `product-service` 和购物车服务 `cart-service`，并通过前缀区分注册中心版本。

## 一、工程模块

| 模块 | 端口 | 作用 | 注册中心 |
| --- | ---: | --- | --- |
| `eureka-server-a` | 8761 | Eureka 注册中心节点 A | Eureka |
| `eureka-server-b` | 8762 | Eureka 注册中心节点 B | Eureka |
| `eureka-product-service` | 8081 | Eureka 版商品服务提供者 | Eureka |
| `eureka-cart-service` | 8082 | Eureka 版购物车服务调用者 | Eureka |
| `nacos-product-service` | 8091 | Nacos 版商品服务提供者 | Nacos |
| `nacos-cart-service` | 8092 | Nacos 版购物车服务调用者 | Nacos |

## 二、实验启动命令

先编译：

```bash
./mvnw clean package -DskipTests
```

启动 Eureka：

```bash
./mvnw -pl eureka-server-a spring-boot:run
./mvnw -pl eureka-server-b spring-boot:run
./mvnw -pl eureka-product-service spring-boot:run
./mvnw -pl eureka-cart-service spring-boot:run
```

启动 Nacos 业务服务：

```bash
./mvnw -pl nacos-product-service spring-boot:run
./mvnw -pl nacos-cart-service spring-boot:run
```

Nacos 集群地址：

- Nacos 节点 1 控制台：`http://127.0.0.1:8848/nacos`
- Nacos 节点 2 控制台：`http://127.0.0.1:18848/nacos`
- Nacos 节点 3 控制台：`http://127.0.0.1:28848/nacos`
- 客户端注册地址：`127.0.0.1:8848,127.0.0.1:18848,127.0.0.1:28848`
- 用户名：`nacos`
- 密码：`20230607`

注意：本实验按三节点 Nacos 集群编写。三个 Nacos Server 需要在服务端使用同一份 `cluster.conf`，并以集群模式启动；客户端配置三个地址用于让微服务接入该 Nacos 集群。

## 三、按实验要求写报告和截图

### （1）新建 Maven 父工程并使用 Dependency Management 锁定版本

代码位置：

- 父工程 [pom.xml](pom.xml)

截图内容：

- 截父工程 `pom.xml`
- 展示 `packaging` 为 `pom`
- 展示 `modules`
- 展示 `properties`
- 展示 `dependencyManagement`

重点版本：

```xml
<spring.boot.version>4.1.0</spring.boot.version>
<spring.cloud.version>2025.1.2</spring.cloud.version>
<spring.cloud.alibaba.version>2025.1.0.0</spring.cloud.alibaba.version>
```

报告可写：

> 本实验创建 Maven 父工程 `demo1`，在父工程中使用 `dependencyManagement` 统一管理 Spring Boot、Spring Cloud 和 Spring Cloud Alibaba 依赖版本，避免各子模块版本不一致。

Eureka 要说明：

- Eureka 相关依赖由 `spring-cloud-dependencies` 管理。
- Eureka Server 使用 `spring-cloud-starter-netflix-eureka-server`。
- Eureka Client 使用 `spring-cloud-starter-netflix-eureka-client`。

Nacos 要说明：

- Nacos 相关依赖由 `spring-cloud-alibaba-dependencies` 管理。
- Nacos Discovery 使用 `spring-cloud-starter-alibaba-nacos-discovery`。

### （2）新建多个 Maven 子工程，实现多个服务调用者和服务消费者

代码位置：

- Eureka 版：
  - `eureka-product-service`
  - `eureka-cart-service`
- Nacos 版：
  - `nacos-product-service`
  - `nacos-cart-service`

截图内容：

- 截 IntelliJ IDEA 左侧 Project 工程结构
- 展示父工程下有多个 Maven 子工程
- 展示每个子工程有独立 `pom.xml`
- 展示每个子工程有 `src/main/java` 和 `src/main/resources/application.yml`

报告可写：

> 本实验创建了多组业务服务。Eureka 组包含 `eureka-product-service` 和 `eureka-cart-service`，Nacos 组包含 `nacos-product-service` 和 `nacos-cart-service`。其中商品服务作为服务提供者，购物车服务作为服务调用者，不同服务使用不同端口运行。

端口说明：

- Eureka 商品服务：`8081`
- Eureka 购物车服务：`8082`
- Nacos 商品服务：`8091`
- Nacos 购物车服务：`8092`

### （3）新建 Maven 子工程，完成服务注册中心实例的创建与配置

Eureka 注册中心代码位置：

- `eureka-server-a`
- `eureka-server-b`

Eureka 截图内容：

- 截 `eureka-server-a/src/main/resources/application.yml`
- 截 `eureka-server-b/src/main/resources/application.yml`
- 展示 `server.port`
- 展示 `eureka.client.service-url.defaultZone`
- 展示 `@EnableEurekaServer`

报告可写：

> 本实验创建了两个 Eureka Server 子工程，分别为 `eureka-server-a` 和 `eureka-server-b`。两个注册中心实例使用不同端口，并通过 `defaultZone` 互相注册，构成 Eureka 注册中心集群。

Nacos 注册中心说明：

- Nacos Server 有三个集群节点：
  - 节点 1：`127.0.0.1:8848`
  - 节点 2：`127.0.0.1:18848`
  - 节点 3：`127.0.0.1:28848`
- 本工程通过 Nacos Discovery 客户端同时配置三个 Nacos 地址。
- 三个 Nacos Server 使用相同的 `cluster.conf`，并保证节点之间网络互通。

Nacos 截图内容：

- 截 `nacos-product-service/src/main/resources/application.yml`
- 截 `nacos-cart-service/src/main/resources/application.yml`
- 展示：
  - `spring.cloud.nacos.discovery.server-addr`
  - `spring.cloud.nacos.username`
  - `spring.cloud.nacos.password`
  - `spring.cloud.nacos.discovery.group`

报告可写：

> Nacos 注册中心由三个本地集群节点组成，地址分别为 `127.0.0.1:8848`、`127.0.0.1:18848`、`127.0.0.1:28848`。Nacos 业务服务通过 `spring.cloud.nacos.discovery.server-addr` 同时配置三个注册中心地址，并将服务注册到 `DEMO1` 分组。

### （4）启动服务注册中心，展示服务注册中心管理后台

Eureka 启动命令：

```bash
./mvnw -pl eureka-server-a spring-boot:run
./mvnw -pl eureka-server-b spring-boot:run
```

Eureka 截图内容：

- 打开 `http://localhost:8761`
- 打开 `http://localhost:8762`
- 两个页面都截图
- 展示页面中的 `EUREKA-CLUSTER`

报告可写：

> 启动 `eureka-server-a` 和 `eureka-server-b` 后，可以分别访问 `http://localhost:8761` 和 `http://localhost:8762` 查看 Eureka 管理后台。两个节点均显示注册中心应用 `EUREKA-CLUSTER`。

Nacos 后台地址：

- `http://127.0.0.1:8848/nacos`
- `http://127.0.0.1:18848/nacos`
- `http://127.0.0.1:28848/nacos`

Nacos 截图内容：

- 分别登录三个 Nacos 节点控制台
- 截控制台首页
- 截“服务管理 / 服务列表”页面

报告可写：

> 本实验配置了三个 Nacos Server 集群节点，分别访问 `http://127.0.0.1:8848/nacos`、`http://127.0.0.1:18848/nacos`、`http://127.0.0.1:28848/nacos` 可以进入 Nacos 管理后台，用于查看服务注册列表和服务实例元数据。

### （5）将多个服务调用者和服务消费者注册到服务注册中心并展示结果

Eureka 启动业务服务：

```bash
./mvnw -pl eureka-product-service spring-boot:run
./mvnw -pl eureka-cart-service spring-boot:run
```

Eureka 截图内容：

- 打开 `http://localhost:8761`
- 打开 `http://localhost:8762`
- 两个后台都要展示：
  - `EUREKA-PRODUCT-SERVICE`
  - `EUREKA-CART-SERVICE`

报告可写：

> Eureka 版商品服务和购物车服务启动后，分别以 `EUREKA-PRODUCT-SERVICE` 和 `EUREKA-CART-SERVICE` 注册到 Eureka。由于客户端配置了两个 Eureka Server 地址，所以两个服务会同时出现在 `8761` 和 `8762` 两个 Eureka 后台中。

Nacos 启动业务服务：

```bash
./mvnw -pl nacos-product-service spring-boot:run
./mvnw -pl nacos-cart-service spring-boot:run
```

Nacos 截图内容：

- 打开 Nacos 控制台
- 进入“服务管理 / 服务列表”
- 命名空间使用默认 `public`
- 分组选择或查看 `DEMO1`
- 展示：
  - `nacos-product-service`
  - `nacos-cart-service`

报告可写：

> Nacos 版商品服务和购物车服务启动后，分别以 `nacos-product-service` 和 `nacos-cart-service` 注册到 Nacos。服务分组为 `DEMO1`，可以在 Nacos 服务列表中查看服务名、实例数和健康状态。

### （6）再次新建服务注册中心实例，完成服务注册中心集群创建和配置

Eureka 集群说明：

- 第一个注册中心：`eureka-server-a`，端口 `8761`
- 第二个注册中心：`eureka-server-b`，端口 `8762`

Eureka 配置截图：

- `eureka-server-a/src/main/resources/application.yml`
- `eureka-server-b/src/main/resources/application.yml`

重点展示：

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8762/eureka/
```

以及：

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

报告可写：

> 为了完成注册中心集群实验，本工程创建了两个 Eureka Server 实例。`eureka-server-a` 将 `defaultZone` 指向 `eureka-server-b`，`eureka-server-b` 将 `defaultZone` 指向 `eureka-server-a`，从而实现注册中心之间的互相注册和数据同步。

Nacos 集群说明：

Nacos 服务端需要三个节点互相感知。三个 Nacos 节点的 `cluster.conf` 中必须写入同一组节点地址，本工程已提供示例文件：[docs/nacos-cluster.conf.example](docs/nacos-cluster.conf.example)。

示例内容：

```text
127.0.0.1:8848
127.0.0.1:18848
127.0.0.1:28848
```

真实 Nacos 集群至少要满足：

- 三个 Nacos Server 都用集群模式启动，而不是 `standalone` 单机模式。
- 三个 Nacos Server 的 `cluster.conf` 节点列表一致。
- 两个节点配置的 IP 和端口必须能被对方访问。
- 生产或正式集群建议使用同一个外部 MySQL 数据库保存配置和元数据。

代码侧 Nacos 客户端已经配置多个注册地址：

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848,127.0.0.1:18848,127.0.0.1:28848
```

Nacos 截图：

- 截三个 Nacos 节点的控制台服务列表
- 截三个 Nacos 节点的 `cluster.conf`，展示节点列表一致
- 如果控制台有“集群管理 / 节点列表”，截图展示两个节点在线
- 截 Nacos 控制台服务列表
- 截 Nacos 服务详情
- 展示服务实例和元数据

报告可写：

> Nacos 注册中心配置了三个本地集群节点。服务端通过 `cluster.conf` 配置相同的节点列表并以集群模式启动，客户端通过 `spring.cloud.nacos.discovery.server-addr` 同时配置多个 Nacos Server 地址。业务服务注册后，可以在 Nacos 控制台中查看服务实例和元数据。

### （7）展示注册中心相互注册过程和微服务元数据备份过程

Eureka 截图内容：

1. 截 `http://localhost:8761`
2. 截 `http://localhost:8762`
3. 两个后台都展示：
   - `EUREKA-CLUSTER`
   - `EUREKA-PRODUCT-SERVICE`
   - `EUREKA-CART-SERVICE`
4. 点开服务实例详情，展示元数据：
   - `module=product-provider`
   - `module=cart-consumer`
   - `business-service=product-service`
   - `business-service=cart-service`
   - `registry=eureka`
   - `version=1.0.0`

Eureka 报告可写：

> Eureka 集群中两个注册中心节点互相注册。业务服务启动后，服务实例和元数据会同步到两个 Eureka Server，因此在 `8761` 和 `8762` 后台都可以看到相同的业务服务实例信息，说明微服务元数据完成了集群内备份。

Nacos 截图内容：

1. 打开 `http://127.0.0.1:8848/nacos`
2. 打开 `http://127.0.0.1:18848/nacos`
3. 打开 `http://127.0.0.1:28848/nacos`
4. 进入“服务管理 / 服务列表”
5. 查看 `DEMO1` 分组
6. 展示：
   - `nacos-product-service`
   - `nacos-cart-service`
7. 点进服务详情，展示元数据：
   - `module=product-provider`
   - `module=cart-consumer`
   - `business-service=product-service`
   - `business-service=cart-service`
   - `registry=nacos`
   - `version=1.0.0`

Nacos 报告可写：

> Nacos 控制台可以查看服务实例列表和实例元数据。`nacos-product-service` 和 `nacos-cart-service` 注册后，在三个 Nacos 节点的控制台中都可以查看服务实例、健康状态和自定义元数据字段。若三个 Nacos Server 已按相同 `cluster.conf` 组成集群，则说明微服务元数据已由 Nacos 集群统一保存和管理。

### （8）使用服务注册中心完成远程服务发现与远程调用

Eureka 调用命令：

```bash
curl http://localhost:8082/api/carts/2001
```

Eureka 调用结果截图重点：

- 返回 HTTP 200
- JSON 中展示：
  - `cartId`
  - `product`
  - `product.name`
  - `product.servedBy=eureka-product-service:8081`
  - `servedBy=eureka-cart-service:8082`
  - `registry=eureka`

Eureka 服务发现验证命令：

```bash
curl http://localhost:8082/api/carts/discovery/eureka-product-service
```

Eureka 服务发现截图重点：

- 返回 `EUREKA-PRODUCT-SERVICE`
- 返回 `host`
- 返回 `port=8081`
- 返回 `metadata`
- `metadata.registry=eureka`

Eureka 报告可写：

> `eureka-cart-service` 通过 Eureka 注册中心发现 `eureka-product-service` 实例，再调用商品服务接口 `/api/products/2001`。调用结果中 `product.servedBy=eureka-product-service:8081` 表明远程服务调用成功。

Nacos 调用命令：

```bash
curl http://localhost:8092/api/carts/2001
```

Nacos 调用结果截图重点：

- 返回 HTTP 200
- JSON 中展示：
  - `cartId`
  - `product`
  - `product.name`
  - `product.servedBy=nacos-product-service:8091`
  - `servedBy=nacos-cart-service:8092`
  - `registry=nacos`

Nacos 服务发现验证命令：

```bash
curl http://localhost:8092/api/carts/discovery/nacos-product-service
```

Nacos 服务发现截图重点：

- 返回 `nacos-product-service`
- 返回 `host`
- 返回 `port=8091`
- 返回 `metadata`
- `metadata.registry=nacos`

Nacos 报告可写：

> `nacos-cart-service` 通过 Nacos 注册中心发现 `nacos-product-service` 实例，再调用商品服务接口 `/api/products/2001`。调用结果中 `product.servedBy=nacos-product-service:8091` 表明基于 Nacos 的远程服务发现和调用成功。

## 四、最终截图顺序建议

报告中建议按下面顺序粘贴截图：

1. 父工程 `pom.xml` 版本管理截图。
2. IDEA 多模块工程结构截图。
3. Eureka Server A 配置截图。
4. Eureka Server B 配置截图。
5. Eureka `8761` 后台截图。
6. Eureka `8762` 后台截图。
7. Eureka 中 `EUREKA-PRODUCT-SERVICE` 和 `EUREKA-CART-SERVICE` 注册结果截图。
8. Eureka 服务实例元数据截图。
9. Nacos 连接配置截图，展示 `127.0.0.1:8848,127.0.0.1:18848,127.0.0.1:28848`。
10. Nacos 节点 1 控制台服务列表截图。
11. Nacos 节点 2 控制台服务列表截图。
12. Nacos 节点 3 控制台服务列表截图。
13. Nacos 中 `nacos-product-service` 和 `nacos-cart-service` 注册结果截图。
14. Nacos 服务实例元数据截图。
15. Eureka 调用结果截图：`curl http://localhost:8082/api/carts/2001`
16. Eureka 服务发现结果截图：`curl http://localhost:8082/api/carts/discovery/eureka-product-service`
17. Nacos 调用结果截图：`curl http://localhost:8092/api/carts/2001`
18. Nacos 服务发现结果截图：`curl http://localhost:8092/api/carts/discovery/nacos-product-service`

## 五、端口占用排查

如果启动失败提示“地址已在使用”，执行：

```bash
ss -ltnp | rg ':(8761|8762|8081|8082|8091|8092)\b'
```

结束对应 Java 进程：

```bash
kill <PID>
```
