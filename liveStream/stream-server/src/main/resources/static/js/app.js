const PlayerState = {
    IDLE: 'IDLE',
    CONNECTING: 'CONNECTING',
    BUFFERING: 'BUFFERING',
    PLAYING: 'PLAYING',
    PAUSED: 'PAUSED',
    ERROR: 'ERROR'
};

const SERVER_URL = '';

class CameraListManager {
    constructor(onSelect) {
        this.onSelect = onSelect;
        this.cameras = [];
        this.activeId = null;
    }

    async refresh() {
        try {
            const resp = await fetch(`${SERVER_URL}/api/agents`);
            if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
            const data = await resp.json();
            this.cameras = data.agents || [];
            this.render();
        } catch (err) {
            console.error('Failed to fetch cameras:', err);
            const list = document.getElementById('cameraList');
            list.innerHTML = `<div class="empty-hint">加载失败: ${err.message}</div>`;
        }
    }

    render() {
        const list = document.getElementById('cameraList');
        if (this.cameras.length === 0) {
            list.innerHTML = '<div class="empty-hint">暂无在线摄像头</div>';
            return;
        }

        list.innerHTML = this.cameras.map(cam => {
            const activeClass = cam.agentId === this.activeId ? ' active' : '';
            const statusClass = (cam.status || '').toLowerCase();
            return `
                <div class="camera-item${activeClass}" data-agent-id="${cam.agentId}">
                    <div class="cam-name">${this.escape(cam.deviceName || cam.agentId)}</div>
                    <div class="cam-id">${this.escape(cam.agentId)}</div>
                    <span class="cam-status ${statusClass}">${this.escape(cam.status || 'UNKNOWN')}</span>
                </div>`;
        }).join('');

        list.querySelectorAll('.camera-item').forEach(item => {
            item.addEventListener('click', () => {
                const agentId = item.dataset.agentId;
                this.setActive(agentId);
                const cam = this.cameras.find(c => c.agentId === agentId);
                if (cam && this.onSelect) this.onSelect(cam);
            });
        });
    }

    setActive(agentId) {
        this.activeId = agentId;
        this.render();
    }

    escape(str) {
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }
}

class ProtocolSelector {
    constructor() {
        this.protocols = {
            hls: new HLSProtocol(),
            httpflv: new HTTPFLVProtocol(),
            webrtc: new WebRTCProtocol()
        };
    }

    select(protocol, config) {
        if (protocol === 'auto') {
            return this.protocols.hls;
        }
        return this.protocols[protocol] || this.protocols.hls;
    }
}

class HLSProtocol {
    constructor() {
        this.hls = null;
    }

    getName() { return 'HLS'; }

    initialize(config) {
        this.config = config;
    }

    async play() {
        const video = document.getElementById('videoPlayer');
        if (typeof Hls !== 'undefined' && Hls.isSupported()) {
            this.hls = new Hls({
                enableWorker: true,
                lowLatencyMode: false
            });
            this.hls.loadSource(this.config.url);
            this.hls.attachMedia(video);
            this.hls.on(Hls.Events.MANIFEST_PARSED, () => {
                video.play();
            });
            this.hls.on(Hls.Events.ERROR, (event, data) => {
                if (data.fatal) {
                    console.error('HLS fatal error:', data);
                    throw new Error('HLS playback failed');
                }
            });
        } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
            video.src = this.config.url;
            video.play();
        } else {
            throw new Error('HLS not supported');
        }
    }

    stop() {
        if (this.hls) {
            this.hls.stopLoad();
            this.hls.destroy();
            this.hls = null;
        }
    }
}

class HTTPFLVProtocol {
    constructor() {
        this.player = null;
    }

    getName() { return 'HTTP-FLV'; }

    initialize(config) {
        this.config = config;
    }

    async play() {
        if (typeof flvjs !== 'undefined' && flvjs.isSupported()) {
            const video = document.getElementById('videoPlayer');
            this.player = flvjs.createPlayer({
                type: 'flv',
                url: this.config.url,
                isLive: true
            });
            this.player.attachMediaElement(video);
            this.player.load();
            this.player.play();
        } else {
            throw new Error('FLV not supported');
        }
    }

    stop() {
        if (this.player) {
            this.player.pause();
            this.player.unload();
            this.player.detachMediaElement();
            this.player.dispose();
            this.player = null;
        }
    }
}

class WebRTCProtocol {
    getName() { return 'WebRTC'; }

    initialize(config) {
        this.config = config;
    }

    async play() {
        try {
            const pc = new RTCPeerConnection({
                iceServers: [{ urls: 'stun:stun.l.google.com:19302' }]
            });
            pc.ontrack = (event) => {
                const video = document.getElementById('videoPlayer');
                video.srcObject = event.streams[0];
                video.play();
            };
            const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
            const video = document.getElementById('videoPlayer');
            video.srcObject = stream;
            await video.play();
            this.pc = pc;
        } catch (error) {
            console.error('WebRTC play error:', error);
            throw error;
        }
    }

    stop() {
        if (this.pc) {
            this.pc.close();
            this.pc = null;
        }
        const video = document.getElementById('videoPlayer');
        if (video.srcObject) {
            video.srcObject.getTracks().forEach(track => track.stop());
            video.srcObject = null;
        }
    }
}

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

class LiveStreamApp {
    constructor() {
        this.selector = new ProtocolSelector();
        this.currentProtocol = null;
        this.state = PlayerState.IDLE;
        this.cameraList = new CameraListManager((cam) => this.onCameraSelect(cam));
        this.heartbeatManager = null;
        this.frontendWs = null;
        this.initElements();
        this.bindEvents();
        this.connectFrontendWebSocket();
    }

    initElements() {
        this.video = document.getElementById('videoPlayer');
        this.streamUrlInput = document.getElementById('streamUrl');
        this.protocolSelect = document.getElementById('protocolSelect');
        this.playBtn = document.getElementById('playBtn');
        this.stopBtn = document.getElementById('stopBtn');
        this.statusEl = document.getElementById('connectionStatus');
        this.refreshBtn = document.getElementById('refreshBtn');
        this.placeholder = document.getElementById('videoPlaceholder');
    }

    bindEvents() {
        this.playBtn.addEventListener('click', () => this.play());
        this.stopBtn.addEventListener('click', () => this.stop());
        this.refreshBtn.addEventListener('click', () => this.cameraList.refresh());
        this.video.addEventListener('pause', () => {
            if (window.app && window.app.heartbeatManager) {
                window.app.heartbeatManager.recordActivity();
            }
        });
        this.video.addEventListener('play', () => {
            if (window.app && window.app.heartbeatManager) {
                window.app.heartbeatManager.recordActivity();
            }
        });
    }

    onCameraSelect(cam) {
        const streamKey = cam.agentId;
        const protocol = this.protocolSelect.value;
        const url = this.buildStreamUrl(protocol, streamKey);
        this.streamUrlInput.value = url || `rtmp://localhost/live/stream-${streamKey}`;
    }

    buildStreamUrl(protocol, streamKey) {
        if (protocol === 'hls') return `http://localhost:8081/hls/${streamKey}.m3u8`;
        if (protocol === 'httpflv') return `http://localhost:8081/flv/${streamKey}.flv`;
        return `http://localhost:8081/hls/${streamKey}.m3u8`;
    }

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

    startHeartbeat(agentId) {
        if (this.heartbeatManager) {
            this.heartbeatManager.stop();
        }
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

    connectFrontendWebSocket() {
        const wsUrl = SERVER_URL ? `ws://${SERVER_URL.replace('http://', '')}/ws/frontend` : 'ws://localhost:8080/ws/frontend';
        const ws = new WebSocket(wsUrl);
        const self = this;
        ws.onmessage = (event) => {
            try {
                const msg = JSON.parse(event.data);
                if (msg.type === 'STREAM_STOPPED') {
                    self.handleStreamStopped(msg.agentId, msg.reason);
                }
            } catch (e) {}
        };
        ws.onclose = () => {
            setTimeout(() => self.connectFrontendWebSocket(), 3000);
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

    updateState(state) {
        this.state = state;
        this.playBtn.disabled = state === PlayerState.PLAYING || state === PlayerState.CONNECTING;
        this.stopBtn.disabled = state === PlayerState.IDLE;

        const statusMap = {
            [PlayerState.IDLE]: { text: '未连接', class: 'disconnected' },
            [PlayerState.CONNECTING]: { text: '推流启动中...', class: 'disconnected' },
            [PlayerState.BUFFERING]: { text: '缓冲中...', class: 'disconnected' },
            [PlayerState.PLAYING]: { text: '播放中', class: 'connected' },
            [PlayerState.ERROR]: { text: '错误', class: 'disconnected' }
        };

        const { text, class: cls } = statusMap[state];
        this.statusEl.textContent = text;
        this.statusEl.className = cls;
    }

    updateStats() {
        setInterval(() => {
            document.getElementById('videoBitrate').textContent = '2000';
            document.getElementById('fps').textContent = '30';
            document.getElementById('latency').textContent = '2000';
            document.getElementById('buffer').textContent = '3000';
        }, 1000);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    window.app = new LiveStreamApp();
});