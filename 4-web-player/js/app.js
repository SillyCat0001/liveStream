const PlayerState = {
    IDLE: 'IDLE',
    CONNECTING: 'CONNECTING',
    BUFFERING: 'BUFFERING',
    PLAYING: 'PLAYING',
    PAUSED: 'PAUSED',
    ERROR: 'ERROR'
};

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

    getName() {
        return 'HLS';
    }

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

    getName() {
        return 'HTTP-FLV';
    }

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
    getName() {
        return 'WebRTC';
    }

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

            // Demo: get local media
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

class LiveStreamApp {
    constructor() {
        this.selector = new ProtocolSelector();
        this.currentProtocol = null;
        this.state = PlayerState.IDLE;
        this.initElements();
        this.bindEvents();
    }

    initElements() {
        this.video = document.getElementById('videoPlayer');
        this.streamUrlInput = document.getElementById('streamUrl');
        this.protocolSelect = document.getElementById('protocolSelect');
        this.playBtn = document.getElementById('playBtn');
        this.stopBtn = document.getElementById('stopBtn');
        this.statusEl = document.getElementById('connectionStatus');
    }

    bindEvents() {
        this.playBtn.addEventListener('click', () => this.play());
        this.stopBtn.addEventListener('click', () => this.stop());
    }

    async play() {
        const url = this.streamUrlInput.value.trim();
        if (!url) {
            alert('请输入流地址');
            return;
        }

        const protocol = this.protocolSelect.value;
        const config = { url };

        this.currentProtocol = this.selector.select(protocol, config);
        this.currentProtocol.initialize(config);
        this.updateState(PlayerState.CONNECTING);

        try {
            await this.currentProtocol.play();
            this.updateState(PlayerState.PLAYING);
            this.updateStats();
        } catch (err) {
            console.error('Playback error:', err);
            this.updateState(PlayerState.ERROR);
        }
    }

    stop() {
        if (this.currentProtocol) this.currentProtocol.stop();
        this.video.src = '';
        this.updateState(PlayerState.IDLE);
    }

    updateState(state) {
        this.state = state;
        this.playBtn.disabled = state === PlayerState.PLAYING;
        this.stopBtn.disabled = state === PlayerState.IDLE;

        const statusMap = {
            [PlayerState.IDLE]: { text: '未连接', class: 'disconnected' },
            [PlayerState.CONNECTING]: { text: '连接中...', class: 'disconnected' },
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