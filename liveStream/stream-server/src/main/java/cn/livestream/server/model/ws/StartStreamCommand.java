package cn.livestream.server.model.ws;

public class StartStreamCommand extends ServerMessage {
    private String rtmpUrl;
    private String streamKey;
    private StreamConfig config;

    public String getRtmpUrl() {
        return rtmpUrl;
    }

    public void setRtmpUrl(String rtmpUrl) {
        this.rtmpUrl = rtmpUrl;
    }

    public String getStreamKey() {
        return streamKey;
    }

    public void setStreamKey(String streamKey) {
        this.streamKey = streamKey;
    }

    public StreamConfig getConfig() {
        return config;
    }

    public void setConfig(StreamConfig config) {
        this.config = config;
    }

    public static class StreamConfig {
        private int width;
        private int height;
        private int bitrate;
        private int fps;

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getBitrate() {
            return bitrate;
        }

        public void setBitrate(int bitrate) {
            this.bitrate = bitrate;
        }

        public int getFps() {
            return fps;
        }

        public void setFps(int fps) {
            this.fps = fps;
        }
    }
}