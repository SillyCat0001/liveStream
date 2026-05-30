# Web-Player 两步式播放 + 心跳续命设计

## 1. 背景与目标

Web-Player 当前点击"播放"按钮后直接播放流地址，但 agent 尚未开始推流，导致播放失败。

**目标：**
- 点击"播放" → 先触发 agent 推流 → agent 确认后开始播放
- 用户离开页面或暂停超过 3 分钟 → 自动停止推流（通过 Redis TTL + 过期事件）
- Server 主动通知前端播放停止，避免前端播空白流

---

## 2. 消息格式

### 2.1 Server → Frontend 新增消息

```json
{
  "type": "STREAM_STOPPED",
  "agentId": "SN-001",
  "reason": "HEARTBEAT_TIMEOUT"
}
```

| reason | 含义 |
|--------|------|
| `HEARTBEAT_TIMEOUT` | Redis key 过期（用户离开/暂停超时） |
| `MANUAL_STOP` | 用户主动点击停止 |

### 2.2 前端 → Server 心跳

```
PUT /api/stream/heartbeat
Body: { "agentId": "SN-001" }
```

成功响应：`200 { "success": true }`  
TTL 被重置为 30s。

---

## 3. Redis 设计

### Key

```
STREAM:HEARTBEAT:{agentId}
Value: agentId
TTL: 30s
```

### 过期事件监听

需要 Redis 配置启用 keyspace notifications：

```
CONFIG SET notify-keyspace-events Ex
```

或 `application.yml` 中配置：

```yaml
spring:
  data:
    redis:
      host: localhost
```

Server 订阅 `__keyevent@0__:expired` 通道。

### 流程

```
PUT /api/stream/heartbeat
  → 重置 STREAM:HEARTBEAT:{agentId} TTL = 30s

STREAM:HEARTBEAT:{agentId} 自然过期（无心跳）
  → Redis 发布过期事件
  → Server 收到过期事件
  → streamCoordinator.stopStream(agentId)
  → Server 主动发 STREAM_STOPPED 到 agent 的 WebSocket
```

---

## 4. 前端行为

### 4.1 播放流程（两步式）

```
用户点击"播放"
  → POST /api/stream/start {agentId}
  → 按钮禁用，状态栏显示"推流启动中..."
  → Server → WebSocket → Agent 收到 START_STREAM
  → Agent 开始推流，回复 COMMAND_RESPONSE
  → Server 回 200，前端收到响应
  → 开始播放流地址（HLS/HTTP-FLV）
  → 状态栏显示"播放中"
  → 启动心跳计时器
```

### 4.2 心跳机制

- 前端记录上一次用户活跃时间（video play/pause 事件）
- 每 10s 检查：
  - 若距离上次活跃时间 < 3 分钟 → 发心跳 `PUT /api/stream/heartbeat`
  - 否则 → 不发心跳
- 用户离开页面（`beforeunload`）→ 不发心跳，key 自然过期

### 4.3 停止流程

```
用户点击"停止"
  → DELETE /api/stream/stop {agentId}
  → 停播放器
  → 停心跳
  → 状态栏显示"未连接"

OR 心跳超时（key 过期）
  → Server 收到 Redis 过期事件
  → streamCoordinator.stopStream(agentId)
  → Server 通过 WebSocket 发 STREAM_STOPPED 给前端
  → 前端停播放器，状态栏显示"推流已停止"，弹窗提示
```

### 4.4 按钮状态

| 状态 | 按钮文字 | 状态栏 |
|------|---------|--------|
| IDLE | 播放 | 未连接 |
| CONNECTING | 播放（禁用） | 推流启动中... |
| PLAYING | 停止推流 | 播放中 |
| ERROR | 播放 | 错误（弹窗） |
| STREAM_STOPPED | 播放 | 推流已停止（弹窗） |

### 4.5 错误处理

- 推流启动失败 → 弹窗 Alert 显示原因
- 播放失败 → 弹窗 Alert，停播放器，恢复按钮

---

## 5. 后端变更

### 5.1 StreamController

新增接口：

```
PUT /api/stream/heartbeat
  → 重置 STREAM:HEARTBEAT:{agentId} TTL = 30s
```

### 5.2 StreamCoordinator

- `startStream()` 中创建 Redis key `STREAM:HEARTBEAT:{agentId}`，TTL=30s
- 新增 Redis key 过期监听逻辑
- 收到过期事件 → `stopStream()` → 主动发 STREAM_STOPPED 消息

### 5.3 AgentWebSocketServer

新增 `STREAM_STOPPED` 消息路由，发送给前端。

### 5.4 application.yml

Redis 配置确保启用 keyspace notifications：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

---

## 6. 前端变更

### 6.1 app.js - LiveStreamApp

- `play()` 方法：
  1. `POST /api/stream/start {agentId}` 获取流信息
  2. 等响应成功 → 开始播放
  3. 启动心跳管理器

- 心跳管理器 `HeartbeatManager`：
  - 每 10s 检查用户活跃状态
  - 活跃 → `PUT /api/stream/heartbeat`
  - 监听 video `pause` 事件，记录暂停时间
  - 暂停超过 3 分钟 → 停止心跳

- `stop()` 方法：
  1. `DELETE /api/stream/stop {agentId}`
  2. 停播放器，停心跳

- 新增 WebSocket 消息处理：`STREAM_STOPPED` → 弹窗 + 停播放器

### 6.2 AgentWebSocketClient（前端）

新增 `STREAM_STOPPED` 处理：

```javascript
case "STREAM_STOPPED":
    app.handleStreamStopped(msg.agentId, msg.reason);
    break;
```

---

## 7. 实现清单

| # | 任务 | 负责 |
|---|------|------|
| 1 | Redis keyspace notifications 配置 | stream-server |
| 2 | `PUT /api/stream/heartbeat` 接口 | stream-server |
| 3 | `StreamCoordinator` 创建/续期 key | stream-server |
| 4 | `StreamCoordinator` 监听 Redis 过期事件 | stream-server |
| 5 | `STREAM_STOPPED` 消息发送到 AgentWebSocketServer | stream-server |
| 6 | 前端 WebSocket 处理 `STREAM_STOPPED` | web-player |
| 7 | 前端 `play()` 改两步式（POST → 等响应 → 播放） | web-player |
| 8 | 前端 `HeartbeatManager` 实现（活跃检测 + 暂停超时） | web-player |
| 9 | 前端按钮合并 + 状态栏文字变化 | web-player |
| 10 | 前端弹窗错误处理 | web-player |

---

## 8. 风险与注意事项

- Redis keyspace notifications 需要 Redis 服务端开启（默认关闭）
- 前端 WebSocket 重连时需重新订阅心跳
- 心跳超时阈值（3 分钟）和 Redis TTL（30s）需配合：TTL 应大于心跳间隔，这里用 30s TTL，每 10s 续一次，安全系数 3x