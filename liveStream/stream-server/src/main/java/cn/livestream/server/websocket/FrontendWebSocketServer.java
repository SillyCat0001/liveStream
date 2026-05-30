package cn.livestream.server.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FrontendWebSocketServer extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(FrontendWebSocketServer.class);
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void sendStreamStopped(String agentId, String reason) {
        String json = String.format(
            "{\"type\":\"STREAM_STOPPED\",\"agentId\":\"%s\",\"reason\":\"%s\"}",
            agentId, reason);
        sessions.values().forEach(session -> {
            try {
                session.sendMessage(new TextMessage(json));
            } catch (Exception e) {
                log.warn("Failed to send STREAM_STOPPED to session {}: {}", session.getId(), e.getMessage());
            }
        });
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 前端暂无需发送消息
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("Transport error for frontend session {}: {}", session.getId(), exception.getMessage());
        sessions.remove(session.getId());
    }
}