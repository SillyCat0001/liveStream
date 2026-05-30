# Web-Player 两步式播放 + 心跳续命 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement two-step playback (POST /api/stream/start → agent push → play) + Redis TTL heartbeat auto-stop + web-player integration into stream-server.

**Architecture:**
- `PUT /api/stream/heartbeat` renews `STREAM:HEARTBEAT:{agentId}` TTL=30s via Redis
- Redis keyspace notification listener detects key expiry → `streamCoordinator.stopStream()` → `STREAM_STOPPED` WebSocket message to frontend
- Frontend uses `HeartbeatManager` (activity-based, 10s interval, 3min pause threshold)
- Web-player static files copied to `stream-server/src/main/resources/static/`

**Tech Stack:** Spring Boot 3.2.5, Spring Data Redis, Spring WebSocket, JavaScript (HLS.js / flv.js)

---

## File Structure

```
stream-server/src/main/java/cn/livestream/server/
├── config/RedisConfig.java             # Add Redis keyspace notification subscription
├── controller/StreamController.java    # Add PUT /api/stream/heartbeat
├── service/StreamCoordinator.java      # Create/renew heartbeat key + Redis expiry listener
├── model/ws/ServerMessage.java         # Add STREAM_STOPPED message type
└── websocket/AgentWebSocketServer.java # Add STREAM_STOPPED routing to frontend

stream-server/src/main/resources/
├── application.yml                     # Redis keyspace notification config
└── static/                             # NEW: web-player static files
    ├── index.html
    ├── css/player.css
    └── js/app.js

4-web-player/                           # Source files (read-only reference)
├── index.html
├── css/player.css
└── js/app.js
```

---

## Task 1: Redis Keyspace Notification 配置

**Files:**
- Modify: `liveStream/stream-server/src/main/resources/application.yml`
- Modify: `liveStream/stream-server/src/main/java/cn/livestream/server/config/RedisConfig.java`

- [ ] **Step 1: 在 application.yml 中启用 Redis keyspace notifications**

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
  redis:
    timeout: 5000
```

- [ ] **Step 2: 修改 RedisConfig，添加 Redis keyspace notification 订阅**

在 `RedisConfig` 中添加 `RedisMessageListenerContainer` bean 用于订阅过期事件通道 `__keyevent@0__:expired`。关键代码：

```java
@Bean
public RedisMessageListenerContainer redisMessageListenerContainer(
        RedisConnectionFactory connectionFactory,
        RedisKeyExpirationListener expirationListener) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    // 订阅 key过期事件通道
    container.addMessageListener(expirationListener,
        new PatternTopic("__keyevent@*__:expired"));
    return container;
}
```

- [ ] **Step 3: 创建 RedisKeyExpirationListener 类**

新建文件 `liveStream/stream-server/src/main/java/cn/livestream/server/config/RedisKeyExpirationListener.java`：

```java
package cn.livestream.server.config;

import cn.livestream.server.service.StreamCoordinator;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
public class RedisKeyExpirationListener implements MessageListener {
    private static final String HEARTBEAT_KEY_PREFIX = "STREAM:HEARTBEAT:";
    private final StreamCoordinator streamCoordinator;

    public RedisKeyExpirationListener(StreamCoordinator streamCoordinator) {
        this.streamCoordinator = streamCoordinator;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        if (expiredKey.startsWith(HEARTBEAT_KEY_PREFIX)) {
            String agentId = expiredKey.substring(HEARTBEAT_KEY_PREFIX.length());
            try {
                streamCoordinator.onHeartbeatExpired(agentId);
            } catch (Exception e) {
                // log error
            }
        }
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add liveStream/stream-server/src/main/resources/application.yml \
  liveStream/stream-server/src/main/java/cn/livestream/server/config/RedisConfig.java \
  liveStream/stream-server/src/main/java/cn/livestream/server/config/RedisKeyExpirationListener.java
git commit -m "feat(stream-server): add Redis keyspace notification listener for heartbeat expiry"
```

---

## Task 2: `PUT /api/stream/heartbeat` 接口

**Files:**
- Modify: `liveStream/stream-server/src/main/java/cn/livestream/server/controller/StreamController.java`

- [ ] **Step 1: 在 StreamController 中添加 heartbeat 方法**

在 `StreamController.java` 的 `DeleteMapping("/stop")` 之后添加：

```java
@PutMapping("/heartbeat")
public ResponseEntity<Map<String, Boolean>> heartbeat(@RequestParam String agentId) {
    try {
        streamCoordinator.renewHeartbeat(agentId);
        Map<String, Boolean> resp = Map.of("success", true);
        return ResponseEntity.ok(resp);
    } catch (Exception e) {
        Map<String, Boolean> resp = Map.of("success", false);
        return ResponseEntity.status(500).body(resp);
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add liveStream/stream-server/src/main/java/cn/livestream/server/controller/StreamController.java
git commit -m "feat(stream-server): add PUT /api/stream/heartbeat endpoint"
```

---

## Task 3: StreamCoordinator 创建/续期 heartbeat key + 过期处理

**Files:**
- Modify: `liveStream/stream-server/src/main/java/cn/livestream/server/service/StreamCoordinator.java`
- Create: `liveStream/stream-server/src/main/java/cn/livestream/server/websocket/FrontendWebSocketServer.java`

- [ ] **Step 1: 修改 StreamCoordinator，添加 Redis 操作**

注入 `StringRedisTemplate`，添加 `HEARTBEAT_KEY_PREFIX = "STREAM:HEARTBEAT:"` 常量，添加 `renewHeartbeat()` 和 `onHeartbeatExpired()` 方法：

```java
private static final String HEARTBEAT_KEY_PREFIX = "STREAM:HEARTBEAT:";
private static final long HEARTBEAT_TTL_SECONDS = 30;
private final StringRedisTemplate redisTemplate;

public void renewHeartbeat(String agentId) {
    String key = HEARTBEAT_KEY_PREFIX + agentId;
    redisTemplate.opsForValue().set(key, agentId, HEARTBEAT_TTL_SECONDS, TimeUnit.SECONDS);
}

public void onHeartbeatExpired(String agentId) {
    try {
        stopStream(agentId);
        // 通知前端
        frontendWebSocketServer.sendStreamStopped(agentId, "HEARTBEAT_TIMEOUT");
    } catch (Exception e) {
        log.error("Failed to handle heartbeat expiry for agent: {}", agentId, e);
    }
}
```

在 `startStream()` 方法末尾添加 `renewHeartbeat(agentId)` 调用。

- [ ] **Step 2: 创建 FrontendWebSocketServer（WebSocket server for frontend）**

新建 `liveStream/stream-server/src/main/java/cn/livestream/server/websocket/FrontendWebSocketServer.java`：

```java
package cn.livestream.server.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class FrontendWebSocketServer extends TextWebSocketHandler {
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void sendStreamStopped(String agentId, String reason) {
        String json = String.format(
            "{\"type\":\"STREAM_STOPPED\",\"agentId\":\"%s\",\"reason\":\"%s\"}",
            agentId, reason);
        sessions.values().forEach(session -> {
            try {
                session.sendMessage(new TextMessage(json));
            } catch (Exception e) {
                // ignore
            }
        });
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 前端暂无需发送消息
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
    }
}
```

- [ ] **Step 3: 在 WebSocketConfig 中注册 frontend ws 端点**

检查 `liveStream/stream-server/src/main/java/cn/livestream/server/config/WebSocketConfig.java`，添加 `/ws/frontend` 的 web socket 端点注册。

- [ ] **Step 4: 提交**

```bash
git add liveStream/stream-server/src/main/java/cn/livestream/server/service/StreamCoordinator.java \
  liveStream/stream-server/src/main/java/cn/livestream/server/websocket/FrontendWebSocketServer.java
git commit -m "feat(stream-server): add heartbeat key renewal and expiry handling in StreamCoordinator"
```

---

## Task 4: AgentWebSocketServer 路由 STREAM_STOPPED 到前端

**Files:**
- Modify: `liveStream/stream-server/src/main/java/cn/livestream/server/websocket/AgentWebSocketServer.java`

- [ ] **Step 1: 在 handleTextMessage 中添加 STREAM_STOPPED 路由**

当 `streamCoordinator.onHeartbeatExpired()` 被调用后，需要 Server → Frontend 发送 `STREAM_STOPPED`。该消息通过 `FrontendWebSocketServer.sendStreamStopped()` 发出，无需在 `AgentWebSocketServer` 中处理。

AgentWebSocketServer 只需将收到的 `STREAM_STOPPED`（来自 agent 的 WebSocket）转发给前端（如果 agent 也需要接收）。根据 spec，STREAM_STOPPED 是 Server → Frontend 的消息，所以只需通过 FrontendWebSocketServer 发送即可。

- [ ] **Step 2: 提交**

```bash
git add liveStream/stream-server/src/main/java/cn/livestream/server/websocket/AgentWebSocketServer.java
git commit -m "refactor(stream-server): clarify STREAM_STOPPED routing (server→frontend only)"
```

---

## Task 5: web-player 集成到 stream-server 静态资源

**Files:**
- Create: `liveStream/stream-server/src/main/resources/static/index.html`
- Create: `liveStream/stream-server/src/main/resources/static/css/player.css`
- Create: `liveStream/stream-server/src/main/resources/static/js/app.js`

- [ ] **Step 1: 复制 index.html**

将 `liveStream/4-web-player/index.html` 复制到 `stream-server/src/main/resources/static/index.html`。

- [ ] **Step 2: 复制 css/player.css**

将 `liveStream/4-web-player/css/player.css` 复制到 `stream-server/src/main/resources/static/css/player.css`。

- [ ] **Step 3: 复制 js/app.js**

将 `liveStream/4-web-player/js/app.js` 复制到 `stream-server/src/main/resources/static/js/app.js`。

- [ ] **Step 4: 修改 app.js 中的 SERVER_URL**

将 `const SERVER_URL = 'http://localhost:8080';` 改为 `const SERVER_URL = '';`（同源，无需指定）。

- [ ] **Step 5: 提交**

```bash
git add liveStream/stream-server/src/main/resources/static/
git commit -m "feat(stream-server): integrate web-player as static resources at /"
```

---

## Task 6: 前端 app.js - 两步式 play() 改造

**Files:**
- Modify: `liveStream/stream-server/src/main/resources/static/js/app.js`

- [ ] **Step 1: 修改 play() 方法为两步式**

原 `play()` 直接播放，改为：

```javascript
async play() {
    const url = this.streamUrlInput.value.trim();
    if (!url) {
        alert('请选择摄像头或输入流地址');
        return;
    }

    const agentId = this.cameraList.activeId;
    if (!agentId) {
        alert('请选择摄像头');
        return;
    }

    // 步骤1：POST /api/stream/start
    this.updateState(PlayerState.CONNECTING);
    this.statusEl.textContent = '推流启动中...';

    try {
        const resp = await fetch(`${SERVER_URL}/api/stream/start`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ agentId })
        });
        const data = await resp.json();
        if (!data.success) {
            alert('推流启动失败: ' + data.message);
            this.updateState(PlayerState.IDLE);
            return;
        }
        // 步骤2：收到成功响应后开始播放
        const streamData = data.data;
        const playUrl = streamData.playUrls.hls || streamData.playUrls.rtmp;
        this.streamUrlInput.value = playUrl;

        const protocol = this.protocolSelect.value;
        const config = { url: playUrl };
        this.currentProtocol = this.selector.select(protocol, config);
        this.currentProtocol.initialize(config);
        this.updateState(PlayerState.PLAYING);
        this.placeholder.style.display = 'none';

        await this.currentProtocol.play();
        this.updateStats();
        this.startHeartbeat(agentId);
    } catch (err) {
        console.error('Playback error:', err);
        this.updateState(PlayerState.ERROR);
        this.placeholder.style.display = 'flex';
        alert('播放失败: ' + err.message);
    }
}
```

- [ ] **Step 2: 修改 stop() 方法，添加 DELETE /api/stream/stop**

```javascript
stop() {
    const agentId = this.cameraList.activeId;
    if (this.currentProtocol) this.currentProtocol.stop();
    this.video.src = '';
    this.video.srcObject = null;
    this.placeholder.style.display = 'flex';
    this.updateState(PlayerState.IDLE);
    this.stopHeartbeat();
    if (agentId) {
        fetch(`${SERVER_URL}/api/stream/stop?agentId=${agentId}`, { method: 'DELETE' })
            .catch(err => console.error('Failed to stop stream:', err));
    }
}
```

- [ ] **Step 3: 添加 HeartbeatManager**

在 `LiveStreamApp` 类中添加 `HeartbeatManager` 相关逻辑：

```javascript
startHeartbeat(agentId) {
    this.heartbeatManager = new HeartbeatManager(agentId, () => {
        this.lastActivityTime = Date.now();
    });
    this.heartbeatManager.start();
}

stopHeartbeat() {
    if (this.heartbeatManager) {
        this.heartbeatManager.stop();
        this.heartbeatManager = null;
    }
}
```

`HeartbeatManager` 类定义在 `LiveStreamApp` 之前：

```javascript
class HeartbeatManager {
    constructor(agentId, onActivity) {
        this.agentId = agentId;
        this.onActivity = onActivity;
        this.interval = null;
        this.lastActivityTime = Date.now();
    }

    start() {
        this.onActivity();
        this.interval = setInterval(() => this.tick(), 10000);
    }

    stop() {
        if (this.interval) {
            clearInterval(this.interval);
            this.interval = null;
        }
    }

    tick() {
        const idle = Date.now() - this.lastActivityTime > 3 * 60 * 1000;
        if (!idle) {
            fetch(`${SERVER_URL}/api/stream/heartbeat?agentId=${this.agentId}`, {
                method: 'PUT'
            }).catch(err => console.error('Heartbeat failed:', err));
        }
    }

    recordActivity() {
        this.lastActivityTime = Date.now();
    }
}
```

在 `bindEvents()` 中添加 video 事件监听：

```javascript
this.video.addEventListener('pause', () => {
    if (window.app && window.app.heartbeatManager) {
        window.app.heartbeatManager.lastActivityTime = Date.now();
    }
});
this.video.addEventListener('play', () => {
    if (window.app && window.app.heartbeatManager) {
        window.app.heartbeatManager.recordActivity();
    }
});
```

- [ ] **Step 4: 添加 STREAM_STOPPED WebSocket 处理**

在页面加载时连接 frontend WebSocket：

```javascript
connectFrontendWebSocket() {
    const ws = new WebSocket(`ws://localhost:8080/ws/frontend`);
    ws.onmessage = (event) => {
        try {
            const msg = JSON.parse(event.data);
            if (msg.type === 'STREAM_STOPPED') {
                this.handleStreamStopped(msg.agentId, msg.reason);
            }
        } catch (e) {}
    };
    ws.onclose = () => {
        setTimeout(() => this.connectFrontendWebSocket(), 3000);
    };
    this.frontendWs = ws;
}

handleStreamStopped(agentId, reason) {
    if (agentId !== this.cameraList.activeId) return;
    if (this.currentProtocol) this.currentProtocol.stop();
    this.video.src = '';
    this.video.srcObject = null;
    this.placeholder.style.display = 'flex';
    this.updateState(PlayerState.IDLE);
    this.stopHeartbeat();
    const msg = reason === 'HEARTBEAT_TIMEOUT'
        ? '推流已停止（离开页面或暂停超时）'
        : '推流已停止';
    alert(msg);
}
```

在 `LiveStreamApp` 构造函数末尾调用 `this.connectFrontendWebSocket();`。

- [ ] **Step 5: 修改 updateState，添加 CONNECTING 状态显示**

```javascript
const statusMap = {
    [PlayerState.IDLE]: { text: '未连接', class: 'disconnected' },
    [PlayerState.CONNECTING]: { text: '推流启动中...', class: 'disconnected' },
    [PlayerState.BUFFERING]: { text: '缓冲中...', class: 'disconnected' },
    [PlayerState.PLAYING]: { text: '播放中', class: 'connected' },
    [PlayerState.ERROR]: { text: '错误', class: 'disconnected' }
};
```

- [ ] **Step 6: 提交**

```bash
git add liveStream/stream-server/src/main/resources/static/js/app.js
git commit -m "feat(web-player): implement two-step play, heartbeat manager, and STREAM_STOPPED handling"
```

---

## Task 7: 前端按钮合并 + 状态栏文字变化

**Files:**
- Modify: `liveStream/stream-server/src/main/resources/static/index.html`
- Modify: `liveStream/stream-server/src/main/resources/static/js/app.js`

- [ ] **Step 1: 修改 index.html，按钮合并为单个**

将 `<button id="playBtn">播放</button>` 和 `<button id="stopBtn">停止</button>` 替换为：

```html
<button id="playBtn" class="btn primary">播放</button>
```

删除 `stopBtn`。

- [ ] **Step 2: 修改 app.js updateState，按钮文字随状态变化**

```javascript
updateState(state) {
    this.state = state;

    const playBtn = this.playBtn;
    const stopBtn = this.stopBtn;

    const stateConfig = {
        [PlayerState.IDLE]: { text: '播放', disabled: false, showStop: false },
        [PlayerState.CONNECTING]: { text: '播放中...', disabled: true, showStop: false },
        [PlayerState.PLAYING]: { text: '停止推流', disabled: false, showStop: true },
        [PlayerState.ERROR]: { text: '播放', disabled: false, showStop: false }
    };

    const config = stateConfig[state] || stateConfig[PlayerState.IDLE];
    playBtn.textContent = config.text;
    playBtn.disabled = config.disabled;
    if (stopBtn) stopBtn.disabled = !config.showStop;

    const statusMap = {
        [PlayerState.IDLE]: { text: '未连接', class: 'disconnected' },
        [PlayerState.CONNECTING]: { text: '推流启动中...', class: 'disconnected' },
        [PlayerState.BUFFERING]: { text: '缓冲中...', class: 'disconnected' },
        [PlayerState.PLAYING]: { text: '播放中', class: 'connected' },
        [PlayerState.ERROR]: { text: '错误', class: 'disconnected' }
    };

    const { text, class: cls } = statusMap[state] || statusMap[PlayerState.IDLE];
    this.statusEl.textContent = text;
    this.statusEl.className = cls;
}
```

- [ ] **Step 3: 修改 play/stop 点击处理，合并为一个按钮**

```javascript
bindEvents() {
    this.playBtn.addEventListener('click', () => {
        if (this.state === PlayerState.PLAYING) {
            this.stop();
        } else {
            this.play();
        }
    });
    // stopBtn 移除
}
```

- [ ] **Step 4: 提交**

```bash
git add liveStream/stream-server/src/main/resources/static/index.html \
  liveStream/stream-server/src/main/resources/static/js/app.js
git commit -m "feat(web-player): merge play/stop buttons, update status bar text"
```

---

## Task 8: 前端 WebSocket 连接 (STREAM_STOPPED)

**Files:**
- Modify: `liveStream/stream-server/src/main/resources/static/js/app.js`
- Modify: `liveStream/stream-server/src/main/java/cn/livestream/server/config/WebSocketConfig.java`

- [ ] **Step 1: 在 WebSocketConfig 中注册 /ws/frontend 端点**

检查现有 WebSocketConfig，确保 `/ws/frontend` 映射到 `FrontendWebSocketServer`。

- [ ] **Step 2: 确保 FrontendWebSocketServer 在 streamCoordinator 之前初始化**

`StreamCoordinator` 需要 `FrontendWebSocketServer`，但 `StreamCoordinator` 是 service 而 `FrontendWebSocketServer` 是 component。确保依赖方向正确。

- [ ] **Step 3: 提交**

```bash
git add liveStream/stream-server/src/main/java/cn/livestream/server/config/WebSocketConfig.java
git commit -m "feat(stream-server): register /ws/frontend WebSocket endpoint"
```

---

## Task 9: 心跳续期 - stopStream 后删除 Redis key

**Files:**
- Modify: `liveStream/stream-server/src/main/java/cn/livestream/server/service/StreamCoordinator.java`

- [ ] **Step 1: 在 stopStream() 中删除 heartbeat key**

```java
public void stopStream(String agentId) throws Exception {
    // 删除 heartbeat key
    String heartbeatKey = HEARTBEAT_KEY_PREFIX + agentId;
    redisTemplate.delete(heartbeatKey);

    StopStreamCommand cmd = new StopStreamCommand();
    cmd.setType("STOP_STREAM");
    String message = objectMapper.writeValueAsString(cmd);
    connectionManager.sendMessage(agentId, message);
    activeStreams.remove(agentId);
    log.info("Stop stream command sent to agent: {}", agentId);
}
```

- [ ] **Step 2: 提交**

```bash
git add liveStream/stream-server/src/main/java/cn/livestream/server/service/StreamCoordinator.java
git commit -m "feat(stream-server): delete heartbeat key when stream stops"
```

---

## Task 10: 启动 Redis keyspace notifications 配置检查

**Files:**
- Modify: `liveStream/stream-server/src/main/resources/application.yml`

- [ ] **Step 1: 在 application.yml 中添加 Spring Boot Redis 配置**

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 5000
```

Note: Redis 服务端需要执行 `CONFIG SET notify-keyspace-events Ex`。在 application.yml 中无法直接设置，需在 Redis 启动时或通过启动脚本执行。

- [ ] **Step 2: 提交**

```bash
git add liveStream/stream-server/src/main/resources/application.yml
git commit -m "chore(stream-server): add Redis connection config to application.yml"
```

---

## Self-Review Checklist

1. **Spec coverage:**
   - ✅ 两步式播放 (Task 6: play() POST then play)
   - ✅ PUT /api/stream/heartbeat (Task 2)
   - ✅ STREAM:HEARTBEAT:{agentId} TTL=30s (Task 3)
   - ✅ Redis keyspace notification listener (Task 1)
   - ✅ StreamCoordinator 监听过期 → stopStream → STREAM_STOPPED (Task 3)
   - ✅ Frontend WebSocket send STREAM_STOPPED (Task 3)
   - ✅ HeartbeatManager 活跃检测 + 暂停超时 (Task 6)
   - ✅ app.js 心跳发送 (Task 6)
   - ✅ 按钮合并 + 状态栏文字 (Task 7)
   - ✅ web-player 集成到 static/ (Task 5)

2. **Placeholder scan:** 无 TBD/TODO/PLACEHOLDER

3. **Type consistency:**
   - `StreamCoordinator.renewHeartbeat(agentId)` - 方法签名在 Task 3 定义，Task 2 调用 ✓
   - `FrontendWebSocketServer.sendStreamStopped(agentId, reason)` - 定义于 Task 3，被 Task 3 内部调用 ✓
   - `HeartbeatManager.recordActivity()` - 定义于 Task 6 ✓