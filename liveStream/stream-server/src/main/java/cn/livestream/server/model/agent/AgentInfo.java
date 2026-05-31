package cn.livestream.server.model.agent;

import cn.livestream.server.model.Capabilities;

public class AgentInfo {
    private String agentId;
    private String deviceName;
    private AgentStatus status;
    private Capabilities capabilities;

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public AgentStatus getStatus() {
        return status;
    }

    public void setStatus(AgentStatus status) {
        this.status = status;
    }

    public Capabilities getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Capabilities capabilities) {
        this.capabilities = capabilities;
    }
}