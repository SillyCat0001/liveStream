package cn.livestream.camera.websocket.model;

public class StatusReport extends WebSocketMessage {
    private String agentId;
    private String status;
    private StreamStats stats = new StreamStats();

    public enum DeviceStatus {
        OFFLINE, ONLINE, STREAMING, ERROR
    }

    public static class StreamStats {
        private int fps;
        private int bitrate;
        private long latencyMs;

        public int getFps() { return fps; }
        public void setFps(int fps) { this.fps = fps; }
        public int getBitrate() { return bitrate; }
        public void setBitrate(int b) { this.bitrate = b; }
        public long getLatencyMs() { return latencyMs; }
        public void setLatencyMs(long ms) { this.latencyMs = ms; }
    }

    public String getAgentId() { return agentId; }
    public void setAgentId(String id) { this.agentId = id; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public StreamStats getStats() { return stats; }
    public void setStats(StreamStats s) { this.stats = s; }
}