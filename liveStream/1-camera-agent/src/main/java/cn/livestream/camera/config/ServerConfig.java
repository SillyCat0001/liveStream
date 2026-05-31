package cn.livestream.camera.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "camera.server")
public class ServerConfig {
    private String websocketUrl = "ws://localhost:8080/ws/agent";
    private int reconnectInterval = 5000;
    private int statusReportInterval = 5000;
    private int maxReconnectAttempts = 10;

    public String getWebsocketUrl() { return websocketUrl; }
    public void setWebsocketUrl(String url) { this.websocketUrl = url; }
    public int getReconnectInterval() { return reconnectInterval; }
    public void setReconnectInterval(int interval) { this.reconnectInterval = interval; }
    public int getStatusReportInterval() { return statusReportInterval; }
    public void setStatusReportInterval(int interval) { this.statusReportInterval = interval; }
    public int getMaxReconnectAttempts() { return maxReconnectAttempts; }
    public void setMaxReconnectAttempts(int n) { this.maxReconnectAttempts = n; }
}
