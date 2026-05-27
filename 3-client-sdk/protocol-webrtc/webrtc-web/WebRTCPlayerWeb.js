// WebRTCPlayerWeb.js - Web 端 WebRTC 播放器实现

export class WebRTCPlayerWeb {
    constructor() {
        this.pc = null;
        this.videoElement = null;
        this.config = null;
        this.stats = {
            videoBitrate: 0,
            fps: 0,
            latencyMs: 0,
            bufferLevelMs: 0
        };
        this.state = 'IDLE';
        this.stateListeners = [];
        this.statsListeners = [];
        this.statTimer = null;
    }

    getName() {
        return 'WebRTC';
    }

    initialize(config) {
        this.config = config;
        this.videoElement = config.videoElement;
        if (!this.videoElement) {
            throw new Error('Video element is required');
        }
        console.log('WebRTCPlayerWeb initialized with URL:', config.streamUrl);
    }

    async play() {
        if (!this.pc && this.config.streamUrl) {
            try {
                this.pc = new RTCPeerConnection({
                    iceServers: [
                        { urls: 'stun:stun.l.google.com:19302' }
                    ]
                });

                this.pc.ontrack = (event) => {
                    if (this.videoElement) {
                        this.videoElement.srcObject = event.streams[0];
                        this.videoElement.play();
                    }
                };

                this.pc.oniceconnectionstatechange = () => {
                    console.log('ICE connection state:', this.pc.iceConnectionState);
                    if (this.pc.iceConnectionState === 'connected') {
                        this._updateState('PLAYING');
                    } else if (this.pc.iceConnectionState === 'failed' ||
                               this.pc.iceConnectionState === 'disconnected') {
                        this._updateState('ERROR');
                    }
                };

                // For demo: simulate play with local video source
                // In production, would connect to WebRTC server
                const stream = await navigator.mediaDevices.getUserMedia({
                    video: true,
                    audio: true
                });
                this.videoElement.srcObject = stream;
                await this.videoElement.play();
                this._updateState('PLAYING');
                this._startStatsMonitoring();

            } catch (error) {
                console.error('WebRTC play error:', error);
                this._updateState('ERROR');
            }
        }
    }

    stop() {
        this._stopStatsMonitoring();
        if (this.pc) {
            this.pc.close();
            this.pc = null;
        }
        if (this.videoElement && this.videoElement.srcObject) {
            const stream = this.videoElement.srcObject;
            stream.getTracks().forEach(track => track.stop());
            this.videoElement.srcObject = null;
        }
        if (this.videoElement) {
            this.videoElement.src = '';
        }
        this._updateState('IDLE');
    }

    pause() {
        if (this.videoElement && this.videoElement.srcObject) {
            this.videoElement.srcObject.getTracks().forEach(track => track.enabled = false);
            this._updateState('PAUSED');
        }
    }

    resume() {
        if (this.videoElement && this.videoElement.srcObject) {
            this.videoElement.srcObject.getTracks().forEach(track => track.enabled = true);
            this._updateState('PLAYING');
        }
    }

    isPlaying() {
        return this.state === 'PLAYING';
    }

    getStats() {
        return { ...this.stats };
    }

    addStateListener(listener) {
        this.stateListeners.push(listener);
    }

    addStatsListener(listener) {
        this.statsListeners.push(listener);
    }

    _startStatsMonitoring() {
        this.statTimer = setInterval(async () => {
            if (this.pc) {
                const stats = await this.pc.getStats();
                stats.forEach(report => {
                    if (report.type === 'inbound-rtp' && report.kind === 'video') {
                        this.stats.videoBitrate = report.bitrateReceived / 1000;
                        this.stats.fps = report.framesPerSecond;
                        this.stats.latencyMs = report.roundTripTime * 1000;
                    }
                });
                this._updateStats();
            }
        }, 1000);
    }

    _stopStatsMonitoring() {
        if (this.statTimer) {
            clearInterval(this.statTimer);
            this.statTimer = null;
        }
    }

    _updateState(newState) {
        this.state = newState;
        this.stateListeners.forEach(l => l(newState));
    }

    _updateStats() {
        this.statsListeners.forEach(l => l(this.stats));
    }
}