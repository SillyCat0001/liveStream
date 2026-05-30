# Web-Player 简化 UI + 只读流地址设计

## 1. 背景与目标

Web-Player 当前页面有"流地址"输入框，用户可以手动修改或输入流地址，这与实际架构不匹配（流地址应由后端返回）。

**目标：**
- 去掉可编辑的流地址输入框，改为只读显示
- 后端 `POST /api/stream/start` 响应中已包含 `playUrls`，前端直接使用
- 保留协议选择下拉框，用户可切换 HLS / HTTP-FLV / 自动选择

---

## 2. UI 变更

### 2.1 index.html

**变更前：**
```html
<div class="control-group">
    <label>流地址:</label>
    <input type="text" id="streamUrl" placeholder="自动填充或手动输入">
</div>
```

**变更后：**
```html
<div class="control-group">
    <label>流地址:</label>
    <input type="text" id="streamUrl" readonly placeholder="自动获取...">
</div>
```

### 2.2 app.js - play() 逻辑

1. 用户选择摄像头 + 协议
2. `POST /api/stream/start {agentId}` 获取 `StreamInfo`
3. 根据协议从 `playUrls` 取对应地址：
   - `hls` → `playUrls.hls`
   - `httpflv` → `playUrls.httpflv`
   - `auto` → 默认 `playUrls.hls`
4. 将地址填入只读 `#streamUrl` 字段
5. 开始播放

### 2.3 按钮状态

| 状态 | 按钮文字 | 状态栏 |
|------|---------|--------|
| IDLE | 播放 | 未连接 |
| CONNECTING | 播放中... | 推流启动中... |
| PLAYING | 停止推流 | 播放中 |
| ERROR | 播放 | 错误 |

---

## 3. 后端无需变更

`POST /api/stream/start` 的 `StreamInfo` 响应已包含 `playUrls`：

```json
{
  "playUrls": {
    "rtmp": "rtmp://localhost/live/stream-SN-001",
    "hls": "http://localhost:8080/live/stream-SN-001.m3u8",
    "httpflv": "http://localhost:8080/live/stream-SN-001.flv"
  }
}
```

---

## 4. 实现清单

| # | 任务 | 文件 |
|---|------|------|
| 1 | `index.html` 流地址输入框改为只读 | `static/index.html` |
| 2 | `app.js` play() 中从 playUrls 取地址填入只读字段 | `static/js/app.js` |
| 3 | `app.js` 协议选择从 playUrls 取对应地址 | `static/js/app.js` |
| 4 | `app.js` onCameraSelect 只填入 agentId 不填 streamUrl | `static/js/app.js` |

---

## 5. 风险与注意事项

- 现有 `playUrls` 结构已满足需求，无需修改后端
- 只读字段不影响播放器正常工作
- 协议选择 "自动选择" 默认使用 HLS