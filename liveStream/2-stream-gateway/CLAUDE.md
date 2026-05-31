[根目录](../../CLAUDE.md) > [liveStream](../) > **2-stream-gateway**

## 模块职责

流媒体网关，对接 IVS (AWS Interactive Video Service) 和 SRS (Simple Realtime Server) 等流媒体服务器。

## 入口与启动

- 入口类: `cn.livestream.gateway.GatewayApplication`
- 启动方式: `mvn spring-boot:run`
- 配置文件: `src/main/resources/application.yml`

## 对外接口

### REST API
- `ChannelController` - 频道管理

### 抽象桥接
- `IVSBridge` - AWS IVS 对接实现
- `SRSBridge` - SRS 对接实现

## 关键依赖

```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>ivs</artifactId>
    <version>2.24.0</version>
</dependency>
```

## 数据模型

- `ChannelInfo` - 频道信息
- `ChannelState` - 频道状态枚举

## 测试与质量

- 无 `src/test/java` 目录

## 常见问题 (FAQ)

- **流媒体服务器对接**: 通过 `StreamGateway` 统一调度，`IVSBridge` / `SRSBridge` 实现具体协议

## 相关文件清单

| 文件 | 说明 |
|------|------|
| `GatewayApplication.java` | Spring Boot 入口 |
| `StreamGateway.java` | 流网关服务 |
| `IVSBridge.java` | IVS 桥接实现 |
| `SRSBridge.java` | SRS 桥接实现 |
| `ChannelController.java` | 频道 REST API |

## 变更记录 (Changelog)

- 2026-05-28: 初始化模块文档