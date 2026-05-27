package cn.livestream.sdk;

public class PlayerStats {
    private int videoBitrate;
    private int audioBitrate;
    private double fps;
    private long latencyMs;
    private int bufferLevelMs;

    public int getVideoBitrate() { return videoBitrate; }
    public void setVideoBitrate(int videoBitrate) { this.videoBitrate = videoBitrate; }
    public int getAudioBitrate() { return audioBitrate; }
    public void setAudioBitrate(int audioBitrate) { this.audioBitrate = audioBitrate; }
    public double getFps() { return fps; }
    public void setFps(double fps) { this.fps = fps; }
    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
    public int getBufferLevelMs() { return bufferLevelMs; }
    public void setBufferLevelMs(int bufferLevelMs) { this.bufferLevelMs = bufferLevelMs; }
}