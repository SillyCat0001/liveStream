[根目录](../../CLAUDE.md) > [liveStream](../) > **stream-server**

## 模块职责

协调服务，接收用户 HTTP 请求，通过 WebSocket 向 camera-agent 下发推流指令。维护 agent 在线状态（Redis）。

## 入口与启动

- 入口类: `cn.livestream.server.ServerApplication`
- 启动方式: `mvn spring-boot:run`
- 配置文件: `src/main/resources/application.yml`

## 对外接口

### HTTP API
- `POST /api/stream/start` - 启动推流
- `DELETE /api/stream/stop?agentId=xxx` - 停止推流
- `GET /api/stream/status/{agentId}` - 查询状态

### WebSocket Server
- `/ws/agent` - 接收 agent 的 REGISTER / HEARTBEAT / COMMAND_RESPONSE

## 关键依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

## 数据模型

### WebSocket 消息
- `RegisterMessage` - agent 注册
- `HeartbeatMessage` - 心跳
- `CommandResponse` - 命令响应
- `StartStreamCommand` - 启动流命令
- `StopStreamCommand` - 停止流命令

### 内部模型
- `AgentInfo` - agent 元数据
- `AgentStatus` - agent 状态 (ONLINE/OFFLINE/STREAMING)
- `StreamInfo` - 流信息

### Redis Key Pattern
- `agent:{agentId}:info` - Hash，agent 元数据
- `agent:{agentId}:status` - String，状态
- `agent:{agentId}:lastHeartbeat` - String，Unix 时间戳

## 测试与质量

- 无 `src/test/java` 目录
- 建议补充: StreamController 单元测试、AgentRegistry 测试

## 常见问题 (FAQ)

- **Agent 注册**: `AgentRegistry` 维护所有在线 agent
- **消息路由**: `AgentConnectionManager` 维护 `sessionId → agentId` 和 `agentId → session` 双向映射

## 相关文件清单

| 文件 | 说明 |
|------|------|
| `ServerApplication.java` | Spring Boot 入口 |
| `StreamController.java` | HTTP REST API |
| `AgentWebSocketServer.java` | WebSocket Server |
| `AgentConnectionManager.java` | 连接管理 |
| `AgentRegistry.java` | agent 注册表 |
| `StreamCoordinator.java` | 流协调器 |

## 变更记录 (Changelog)

- 2026-05-28: 初始化模块文档