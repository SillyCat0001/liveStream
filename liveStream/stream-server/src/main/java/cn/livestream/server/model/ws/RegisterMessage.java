package cn.livestream.server.model.ws;

import cn.livestream.server.model.Capabilities;

public class RegisterMessage extends AgentMessage {
    private String agentId;
    private String deviceInfo;
    private String protocolVersion = "1.0";
    private Capabilities capabilities = new Capabilities();

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public Capabilities getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Capabilities capabilities) {
        this.capabilities = capabilities;
    }
}