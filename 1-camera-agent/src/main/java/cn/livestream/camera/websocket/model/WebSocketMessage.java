package cn.livestream.camera.websocket.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = RegisterMessage.class, name = "REGISTER"),
    @JsonSubTypes.Type(value = StartStreamCommand.class, name = "START_STREAM"),
    @JsonSubTypes.Type(value = StatusReport.class, name = "STATUS_REPORT"),
    @JsonSubTypes.Type(value = CommandResponse.class, name = "COMMAND_RESPONSE")
})
public abstract class WebSocketMessage {
    private String type;
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}