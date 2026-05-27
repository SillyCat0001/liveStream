package cn.livestream.camera.websocket.model;

public class CommandResponse extends WebSocketMessage {
    private String originalType;
    private boolean success;
    private String message;

    public String getOriginalType() { return originalType; }
    public void setOriginalType(String t) { this.originalType = t; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean s) { this.success = s; }
    public String getMessage() { return message; }
    public void setMessage(String m) { this.message = m; }
}