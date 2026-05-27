# Camera Agent WebSocket 远程控制改造设计文档

> 日期: 2026-05-28
> 状态: 已批准

## 1. 系统架构

```
┌──────────────────────────────────────────────────────────────────┐
│                        Camera Agent                                │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐        │
│  │   Startup   │────▶│  WebSocket │────▶│  Register  │        │
│  │  (启动)     │     │  Client    │     │  to Server │        │
│  └─────────────┘     └──────┬──────┘     └─────────────┘        │
│                             │                                      │
│                             ▼                                      │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐        │
│  │  RTMP       │◀────│   Message   │◀────│   Remote    │        │
│  │  Pusher     │     │   Handler   │     │   Server    │        │
│  └─────────────┘     └─────────────┘     └─────────────┘        │
│                             │                                      │
│                             ▼                                      │
│                      ┌─────────────┐                              │
│                      │  Status     │──────────────────▶ 上报状态 │
│                      │  Reporter   │                              │
│                      └─────────────┘                              │
└──────────────────────────────────────────────────────────────────┘
```

## 2. 消息格式

### 2.1 注册消息 (Client → Server)

**时机:** WebSocket 连接建立后立即发送

```json
{
  "type": "REGISTER",
  "deviceId": "camera-001",
  "name": "Front Door Camera",
  "protocolVersion": "1.0",
  "capabilities": {
    "protocols": ["RTMP"],
    "maxResolution": "1920x1080",
    "maxFps": 30,
    "codecs": ["h264"]
  }
}
```

### 2.2 推流命令 (Server → Client)

**时机:** 服务器主动发送，触发摄像头开始推流

```json
{
  "type": "START_STREAM",
  "streamKey": "live/abc123",
  "rtmpUrl": "rtmp://your-server.com",
  "config": {
    "width": 1920,
    "height": 1080,
    "fps": 30,
    "bitrate": 2000
  }
}
```

**停止推流命令:**

```json
{
  "type": "STOP_STREAM"
}
```

### 2.3 状态上报 (Client → Server)

**时机:** 设备定时上报（每 5 秒）或状态变化时立即上报

```json
{
  "type": "STATUS_REPORT",
  "deviceId": "camera-001",
  "status": "STREAMING",
  "stats": {
    "fps": 30,
    "bitrate": 2000,
    "latencyMs": 100
  }
}
```

**状态枚举:** `OFFLINE`, `ONLINE`, `STREAMING`, `ERROR`

### 2.4 命令响应 (Client → Server)

**时机:** 收到服务器命令后回应

```json
{
  "type": "COMMAND_RESPONSE",
  "originalType": "START_STREAM",
  "success": true,
  "message": "Stream started"
}
```

## 3. 核心组件

| 组件 | 职责 |
|------|------|
| **WebSocketClient** | WebSocket 连接管理、自动重连、心跳 |
| **RegistrationService** | 启动时发送注册消息 |
| **StreamMessageHandler** | 处理 START_STREAM / STOP_STREAM 命令 |
| **StatusReporter** | 定时收集并上报设备状态 |
| **RTMPPusher** | 现有推流组件（复用） |

## 4. 配置项

### 4.1 application.yml

```yaml
camera:
  device-id: camera-001
  device-name: Front Door Camera
  server:
    websocket-url: ws://your-server.com:8080/ws
    reconnect-interval: 5000
    status-report-interval: 5000
```

## 5. 技术实现

### 5.1 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

### 5.2 WebSocketClient 接口

```java
public interface WebSocketClient {
    void connect();
    void disconnect();
    void sendMessage(String message);
    void onMessage(Consumer<String> handler);
    boolean isConnected();
}
```

### 5.3 StreamMessageHandler 处理逻辑

```
收到 START_STREAM
    ↓
解析 rtmpUrl, streamKey, config
    ↓
更新 CameraConfig
    ↓
调用 RTMPPusher.start()
    ↓
发送 COMMAND_RESPONSE (success/failure)
    ↓
启动 StatusReporter

收到 STOP_STREAM
    ↓
调用 RTMPPusher.stop()
    ↓
发送 COMMAND_RESPONSE
    ↓
停止 StatusReporter
```

## 6. 改造范围

| 文件 | 操作 | 说明 |
|------|------|------|
| `pom.xml` | 修改 | 添加 spring-boot-starter-websocket 依赖 |
| `CameraConfig.java` | 修改 | 添加 device-id, device-name, server 配置 |
| `WebSocketClient.java` | 新增 | WebSocket 连接管理 |
| `RegistrationService.java` | 新增 | 注册逻辑 |
| `StreamMessageHandler.java` | 新增 | 消息处理 |
| `StatusReporter.java` | 新增 | 状态上报 |
| `CameraApplication.java` | 修改 | 启动时初始化 WebSocket 连接 |

## 7. 错误处理

| 场景 | 处理方式 |
|------|----------|
| WebSocket 连接失败 | 指数退避重连 (1s, 2s, 4s, 8s, 最大 30s) |
| 推流失败 | 发送 ERROR 状态，主动断开重试 |
| 服务器无响应 | 心跳检测 (30s 无响应则重连) |
| 配置解析失败 | 发送 ERROR 响应给服务器 |