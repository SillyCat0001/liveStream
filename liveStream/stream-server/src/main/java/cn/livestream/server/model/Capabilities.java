package cn.livestream.server.model;

public class Capabilities {
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