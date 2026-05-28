package cn.livestream.server.model.ws;

public abstract class AgentMessage {
    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}