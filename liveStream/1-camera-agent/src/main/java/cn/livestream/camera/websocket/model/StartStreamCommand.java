package cn.livestream.camera.websocket.model;

public class StartStreamCommand extends WebSocketMessage {
    private String streamKey;
    private String rtmpUrl;
    private StreamConfig config = new StreamConfig();

    public static class StreamConfig {
        private int width = 1920;
        private int height = 1080;
        private int fps = 30;
        private int bitrate = 2000;

        public int getWidth() { return width; }
        public void setWidth(int w) { this.width = w; }
        public int getHeight() { return height; }
        public void setHeight(int h) { this.height = h; }
        public int getFps() { return fps; }
        public void setFps(int fps) { this.fps = fps; }
        public int getBitrate() { return bitrate; }
        public void setBitrate(int b) { this.bitrate = b; }
    }

    public String getStreamKey() { return streamKey; }
    public void setStreamKey(String k) { this.streamKey = k; }
    public String getRtmpUrl() { return rtmpUrl; }
    public void setRtmpUrl(String url) { this.rtmpUrl = url; }
    public StreamConfig getConfig() { return config; }
    public void setConfig(StreamConfig c) { this.config = c; }
}