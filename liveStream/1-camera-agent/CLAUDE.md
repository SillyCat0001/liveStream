[根目录](../../CLAUDE.md) > [liveStream](../) > **1-camera-agent**

## 模块职责

推流端 Agent，接收 `stream-server` 的 WebSocket 命令，控制 FFmpeg 进行 RTMP 推流。

## 入口与启动

- 入口类: `cn.livestream.camera.CameraApplication`
- 启动方式: `mvn spring-boot:run`
- 配置文件: `src/main/resources/application.yml`

## 对外接口

### WebSocket (客户端)
- 连接 `stream-server` 的 `/ws/agent` 端点
- 接收命令: `START_STREAM`, `STOP_STREAM`
- 发送消息: `REGISTER`, `HEARTBEAT`, `COMMAND_RESPONSE`

### HTTP (健康检查)
- `GET /health` - 健康状态

## 关键依赖

```xml
<dependency>
    <groupId>org.bytedeco</groupId>
    <artifactId>javacv-platform</artifactId>
    <version>1.5.10</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

## 数据模型

### WebSocket 消息模型
- `RegisterMessage` - 注册消息，含设备能力
- `StartStreamCommand` - 启动流命令，含推流地址和流名称
- `StopStreamCommand` - 停止流命令
- `StatusReport` - 状态上报，含设备状态和流统计
- `CommandResponse` - 命令响应

## 测试与质量

- 无 `src/test/java` 目录
- 建议补充: WebSocket 重连机制测试、FFmpegWrapper 单元测试

## 常见问题 (FAQ)

- **FFmpeg 路径**: 通过 `CameraConfig` 配置
- **重连机制**: `StandardWebSocketClient` 实现，5秒间隔，最多10次

## 相关文件清单

| 文件 | 说明 |
|------|------|
| `CameraApplication.java` | Spring Boot 入口 |
| `StandardWebSocketClient.java` | WebSocket 客户端，含重连 |
| `FFmpegWrapper.java` | FFmpeg 封装 |
| `RTMPPusher.java` | RTMP 推流实现 |
| `StreamMessageHandler.java` | WS 消息处理 |

## 变更记录 (Changelog)

- 2026-05-28: 初始化模块文档