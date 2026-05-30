# Web-Player UI Simplify Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 web-player 页面的流地址输入框改为只读，后端响应中直接获取播放地址填充到字段中显示。

**Architecture:** 前端从 `POST /api/stream/start` 响应中获取 `playUrls`，根据协议选择填充到只读输入框，不依赖用户手动输入。

**Tech Stack:** Vanilla JavaScript (HLS.js / flv.js), HTML

---

## File Structure

```
stream-server/src/main/resources/static/
├── index.html    # 修改：流地址输入框加 readonly
└── js/app.js     # 修改：play() 逻辑、从 playUrls 取地址、onCameraSelect 不再填充 streamUrl
```

---

## Task 1: index.html 流地址输入框改为只读

**Files:**
- Modify: `stream-server/src/main/resources/static/index.html`

- [ ] **Step 1: 修改流地址输入框属性**

找到第 42 行：
```html
<input type="text" id="streamUrl" placeholder="自动填充或手动输入">
```

改为：
```html
<input type="text" id="streamUrl" readonly placeholder="自动获取...">
```

- [ ] **Step 2: 提交**

```bash
git add liveStream/stream-server/src/main/resources/static/index.html
git commit -m "feat(web-player): make stream URL input readonly"
```

---

## Task 2: app.js onCameraSelect 不再填充 streamUrl

**Files:**
- Modify: `stream-server/src/main/resources/static/js/app.js`

- [ ] **Step 1: 修改 onCameraSelect 方法**

找到当前 `onCameraSelect` 方法（约第 289 行）：
```javascript
onCameraSelect(cam) {
    const streamKey = cam.agentId;
    const protocol = this.protocolSelect.value;
    const url = this.buildStreamUrl(protocol, streamKey);
    this.streamUrlInput.value = url || `rtmp://localhost/live/stream-${streamKey}`;
}
```

改为：
```javascript
onCameraSelect(cam) {
    // 只设置 activeId，不填充 streamUrl（由 play() 从后端 playUrls 获取）
    this.streamUrlInput.value = '';
}
```

- [ ] **Step 2: 提交**

```bash
git add liveStream/stream-server/src/main/resources/static/js/app.js
git commit -m "feat(web-player): onCameraSelect no longer fills streamUrl manually"
```

---

## Task 3: app.js play() 中根据协议从 playUrls 取地址填入只读字段

**Files:**
- Modify: `stream-server/src/main/resources/static/js/app.js`

- [ ] **Step 1: 修改 play() 中解析 playUrls 的逻辑**

在 `play()` 方法中，POST 成功后的响应解析部分（约第 333 行），将：
```javascript
const streamData = data.data;
const playUrl = streamData.playUrls.hls || streamData.playUrls.rtmp;
this.streamUrlInput.value = playUrl;
```

改为：
```javascript
const streamData = data.data;
const protocol = this.protocolSelect.value;
const playUrls = streamData.playUrls || {};
let playUrl;
if (protocol === 'hls') {
    playUrl = playUrls.hls;
} else if (protocol === 'httpflv') {
    playUrl = playUrls.httpflv;
} else {
    playUrl = playUrls.hls || playUrls.rtmp;
}
this.streamUrlInput.value = playUrl || '';
```

- [ ] **Step 2: 提交**

```bash
git add liveStream/stream-server/src/main/resources/static/js/app.js
git commit -m "feat(web-player): play() selects URL from playUrls based on protocol"
```

---

## Self-Review Checklist

1. **Spec coverage:**
   - ✅ 流地址输入框改为只读（Task 1）
   - ✅ play() 从 playUrls 取地址填入只读字段（Task 3）
   - ✅ onCameraSelect 不再填充 streamUrl（Task 2）
   - ✅ 协议选择从 playUrls 取对应地址（Task 3）

2. **Placeholder scan:** 无 TBD/TODO/PLACEHOLDER

3. **Type consistency:** playUrls.hls / playUrls.httpflv 与后端 StreamInfo 返回结构一致