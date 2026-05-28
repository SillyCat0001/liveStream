package cn.livestream.server.config;

import cn.livestream.server.websocket.AgentWebSocketServer;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final AgentWebSocketServer agentWebSocketServer;

    public WebSocketConfig(AgentWebSocketServer agentWebSocketServer) {
        this.agentWebSocketServer = agentWebSocketServer;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(agentWebSocketServer, "/ws/agent")
            .setAllowedOrigins("*");
    }
}