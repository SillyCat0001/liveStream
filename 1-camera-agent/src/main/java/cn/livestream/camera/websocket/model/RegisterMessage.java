package cn.livestream.camera.websocket.model;

public class RegisterMessage extends WebSocketMessage {
    private String deviceId;
    private String name;
    private String protocolVersion = "1.0";
    private Capabilities capabilities = new Capabilities();

    public static class Capabilities {
        private String[] protocols = {"RTMP"};
        private String maxResolution = "1920x1080";
        private int maxFps = 30;
        private String[] codecs = {"h264"};

        public String[] getProtocols() { return protocols; }
        public void setProtocols(String[] p) { this.protocols = p; }
        public String getMaxResolution() { return maxResolution; }
        public void setMaxResolution(String r) { this.maxResolution = r; }
        public int getMaxFps() { return maxFps; }
        public void setMaxFps(int fps) { this.maxFps = fps; }
        public String[] getCodecs() { return codecs; }
        public void setCodecs(String[] c) { this.codecs = c; }
    }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String id) { this.deviceId = id; }
    public String getName() { return name; }
    public void setName(String n) { this.name = n; }
    public String getProtocolVersion() { return protocolVersion; }
    public void setProtocolVersion(String v) { this.protocolVersion = v; }
    public Capabilities getCapabilities() { return capabilities; }
    public void setCapabilities(Capabilities c) { this.capabilities = c; }
}