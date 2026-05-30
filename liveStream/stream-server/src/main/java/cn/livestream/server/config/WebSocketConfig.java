package cn.livestream.server.config;

import cn.livestream.server.websocket.AgentWebSocketServer;
import cn.livestream.server.websocket.FrontendWebSocketServer;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final AgentWebSocketServer agentWebSocketServer;
    private final FrontendWebSocketServer frontendWebSocketServer;

    public WebSocketConfig(AgentWebSocketServer agentWebSocketServer,
                           FrontendWebSocketServer frontendWebSocketServer) {
        this.agentWebSocketServer = agentWebSocketServer;
        this.frontendWebSocketServer = frontendWebSocketServer;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(agentWebSocketServer, "/ws/agent")
            .setAllowedOrigins("*");
        registry.addHandler(frontendWebSocketServer, "/ws/frontend")
            .setAllowedOrigins("*");
    }
}