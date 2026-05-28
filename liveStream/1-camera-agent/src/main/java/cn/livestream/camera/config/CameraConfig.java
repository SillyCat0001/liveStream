package cn.livestream.camera.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "camera")
public class CameraConfig {
    private String deviceId = "camera-001";
    private String deviceName = "Camera Device";
    private String rtmpUrl = "rtmp://localhost/live";
    private String streamKey = "test-stream";
    private int width = 1920;
    private int height = 1080;
    private int fps = 30;
    private int videoBitrate = 2000;
    private int audioBitrate = 128;
    private String codec = "h264";
    private ServerConfig server = new ServerConfig();

    public static class ServerConfig {
        private String websocketUrl = "ws://localhost:8080/ws";
        private int reconnectInterval = 5000;
        private int statusReportInterval = 5000;
        private int maxReconnectAttempts = 10;

        public String getWebsocketUrl() { return websocketUrl; }
        public void setWebsocketUrl(String url) { this.websocketUrl = url; }
        public int getReconnectInterval() { return reconnectInterval; }
        public void setReconnectInterval(int interval) { this.reconnectInterval = interval; }
        public int getStatusReportInterval() { return statusReportInterval; }
        public void setStatusReportInterval(int interval) { this.statusReportInterval = interval; }
        public int getMaxReconnectAttempts() { return maxReconnectAttempts; }
        public void setMaxReconnectAttempts(int n) { this.maxReconnectAttempts = n; }
    }

    // getters and setters
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String id) { this.deviceId = id; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String name) { this.deviceName = name; }
    public String getRtmpUrl() { return rtmpUrl; }
    public void setRtmpUrl(String url) { this.rtmpUrl = url; }
    public String getStreamKey() { return streamKey; }
    public void setStreamKey(String key) { this.streamKey = key; }
    public int getWidth() { return width; }
    public void setWidth(int w) { this.width = w; }
    public int getHeight() { return height; }
    public void setHeight(int h) { this.height = h; }
    public int getFps() { return fps; }
    public void setFps(int fps) { this.fps = fps; }
    public int getVideoBitrate() { return videoBitrate; }
    public void setVideoBitrate(int bitrate) { this.videoBitrate = bitrate; }
    public int getAudioBitrate() { return audioBitrate; }
    public void setAudioBitrate(int bitrate) { this.audioBitrate = bitrate; }
    public String getCodec() { return codec; }
    public void setCodec(String codec) { this.codec = codec; }
    public ServerConfig getServer() { return server; }
    public void setServer(ServerConfig server) { this.server = server; }
}