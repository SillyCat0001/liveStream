package cn.livestream.camera.websocket.model;

import cn.livestream.camera.model.Capabilities;

public class RegisterMessage extends WebSocketMessage {
    private String agentId;
    private String deviceInfo;
    private String protocolVersion = "1.0";
    private Capabilities capabilities = new Capabilities();

    public String getAgentId() { return agentId; }
    public void setAgentId(String id) { this.agentId = id; }
    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String info) { this.deviceInfo = info; }
    public String getProtocolVersion() { return protocolVersion; }
    public void setProtocolVersion(String v) { this.protocolVersion = v; }
    public Capabilities getCapabilities() { return capabilities; }
    public void setCapabilities(Capabilities c) { this.capabilities = c; }
}