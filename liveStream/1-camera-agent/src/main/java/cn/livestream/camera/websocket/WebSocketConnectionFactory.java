package cn.livestream.camera.websocket;

import cn.livestream.camera.config.ServerConfig;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

@Component
public class WebSocketConnectionFactory {
    private final String websocketUrl;

    public WebSocketConnectionFactory(ServerConfig serverConfig) {
        this.websocketUrl = serverConfig.getWebsocketUrl();
    }

    public WebSocketConnectionManager create(WebSocketHandler handler) {
        WebSocketConnectionManager manager = new WebSocketConnectionManager(
            new StandardWebSocketClient(),
            handler,
            websocketUrl
        );
        return manager;
    }
}
