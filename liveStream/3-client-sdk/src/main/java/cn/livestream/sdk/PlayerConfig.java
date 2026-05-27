package cn.livestream.sdk;

public class PlayerConfig {
    private String streamUrl;
    private boolean autoSwitchProtocol = true;
    private int bufferMs = 3000;
    private int maxBufferMs = 10000;
    private String preferredProtocol = "auto";

    public String getStreamUrl() { return streamUrl; }
    public void setStreamUrl(String streamUrl) { this.streamUrl = streamUrl; }
    public boolean isAutoSwitchProtocol() { return autoSwitchProtocol; }
    public void setAutoSwitchProtocol(boolean autoSwitchProtocol) { this.autoSwitchProtocol = autoSwitchProtocol; }
    public int getBufferMs() { return bufferMs; }
    public void setBufferMs(int bufferMs) { this.bufferMs = bufferMs; }
    public int getMaxBufferMs() { return maxBufferMs; }
    public void setMaxBufferMs(int maxBufferMs) { this.maxBufferMs = maxBufferMs; }
    public String getPreferredProtocol() { return preferredProtocol; }
    public void setPreferredProtocol(String preferredProtocol) { this.preferredProtocol = preferredProtocol; }
}