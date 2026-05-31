package cn.livestream.server.model.ws;

public class StatusReport extends AgentMessage {
    private String agentId;
    private String status;
    private StreamStats stats;

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public StreamStats getStats() {
        return stats;
    }

    public void setStats(StreamStats stats) {
        this.stats = stats;
    }

    public static class StreamStats {
        private int fps;
        private int bitrate;
        private long latencyMs;

        public int getFps() {
            return fps;
        }

        public void setFps(int fps) {
            this.fps = fps;
        }

        public int getBitrate() {
            return bitrate;
        }

        public void setBitrate(int bitrate) {
            this.bitrate = bitrate;
        }

        public long getLatencyMs() {
            return latencyMs;
        }

        public void setLatencyMs(long latencyMs) {
            this.latencyMs = latencyMs;
        }
    }
}