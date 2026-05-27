// HLSPlayerWeb.js - Web HLS Player Implementation using hls.js
import Hls from 'hls.js';

export class HLSPlayerWeb {
    constructor() {
        this.hls = null;
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
    }

    getName() {
        return 'HLS';
    }

    initialize(config) {
        this.config = config;
        this.videoElement = config.videoElement;
        if (!this.videoElement) {
            throw new Error('Video element is required');
        }
        console.log('HLSPlayerWeb initialized with URL:', config.streamUrl);
    }

    play() {
        if (!this.hls && this.config.streamUrl) {
            if (Hls.isSupported()) {
                this.hls = new Hls({
                    enableWorker: true,
                    lowLatencyMode: false,
                    backBufferLength: 30
                });

                this.hls.loadSource(this.config.streamUrl);
                this.hls.attachMedia(this.videoElement);

                this.hls.on(Hls.Events.MANIFEST_PARSED, (event, data) => {
                    this._updateState('PLAYING');
                    this.videoElement.play().catch(e => {
                        console.error('Autoplay failed:', e);
                        this._updateState('PAUSED');
                    });
                });

                this.hls.on(Hls.Events.ERROR, (event, data) => {
                    if (data.fatal) {
                        console.error('HLS fatal error:', data);
                        this._updateState('ERROR');
                    }
                });

                this.hls.on(Hls.Events.FRAG_LOADED, (event, data) => {
                    this._updateStats();
                });
            } else if (this.videoElement.canPlayType('application/vnd.apple.mpegurl')) {
                this.videoElement.src = this.config.streamUrl;
                this.videoElement.addEventListener('loadedmetadata', () => {
                    this._updateState('PLAYING');
                    this.videoElement.play();
                });
            }
        }
    }

    stop() {
        if (this.hls) {
            this.hls.stopLoad();
            this.hls.destroy();
            this.hls = null;
        }
        if (this.videoElement) {
            this.videoElement.src = '';
        }
        this._updateState('IDLE');
    }

    pause() {
        if (this.videoElement) {
            this.videoElement.pause();
            this._updateState('PAUSED');
        }
    }

    resume() {
        if (this.videoElement) {
            this.videoElement.play();
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

    _updateState(newState) {
        this.state = newState;
        this.stateListeners.forEach(l => l(newState));
    }

    _updateStats() {
        if (this.hls) {
            const level = this.hls.levels[this.hls.currentLevel];
            if (level) {
                this.stats.videoBitrate = level.bitrate / 1000;
            }
        }
        this.statsListeners.forEach(l => l(this.stats));
    }
}