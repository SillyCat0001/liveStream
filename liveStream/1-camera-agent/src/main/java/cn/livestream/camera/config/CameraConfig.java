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
    private String sn = "";

    public String getSn() { return sn; }
    public void setSn(String sn) { this.sn = sn; }

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
}