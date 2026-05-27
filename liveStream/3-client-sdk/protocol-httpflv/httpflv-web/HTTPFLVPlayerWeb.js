// HTTPFLVPlayerWeb.js - Web 端 HTTP-FLV 播放器实现
import flvjs from 'flv.js';

export class HTTPFLVPlayerWeb {
    constructor() {
        this.player = null;
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
        return 'HTTP-FLV';
    }

    initialize(config) {
        this.config = config;
        this.videoElement = config.videoElement;
        if (!this.videoElement) {
            throw new Error('Video element is required');
        }
        console.log('HTTPFLVPlayerWeb initialized with URL:', config.streamUrl);
    }

    play() {
        if (!this.player && this.config.streamUrl) {
            if (flvjs.isSupported()) {
                this.player = flvjs.createPlayer({
                    type: 'flv',
                    url: this.config.streamUrl,
                    isLive: true,
                    hasAudio: true,
                    hasVideo: true
                }, {
                    enableWorker: true,
                    enableStashBuffer: false,
                    stashInitialSize: 128
                });

                this.player.attachMediaElement(this.videoElement);
                this.player.load();
                this.player.play();

                this.player.on(flvjs.Events.ERROR, (errType, errDetail) => {
                    console.error('FLV player error:', errType, errDetail);
                    this._updateState('ERROR');
                });

                this.player.on(flvjs.Events.STATISTICS_UPDATE, (data) => {
                    this.stats.videoBitrate = data.bitrate / 1000;
                    this.stats.fps = data.fps;
                    this._updateStats();
                });

                this._updateState('PLAYING');
            }
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
        if (this.videoElement) {
            this.videoElement.src = '';
        }
        this._updateState('IDLE');
    }

    pause() {
        if (this.player) {
            this.player.pause();
            this._updateState('PAUSED');
        }
    }

    resume() {
        if (this.player) {
            this.player.play();
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
        this.statsListeners.forEach(l => l(this.stats));
    }
}