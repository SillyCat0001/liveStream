package cn.livestream.server.service;

import org.springframework.web.socket.WebSocketSession;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AgentConnectionManager {
    private final Map<String, WebSocketSession> agentSessions = new ConcurrentHashMap<>();

    public void register(String agentId, WebSocketSession session) {
        agentSessions.put(agentId, session);
    }

    public void unregister(String agentId) {
        agentSessions.remove(agentId);
    }

    public WebSocketSession getSession(String agentId) {
        return agentSessions.get(agentId);
    }

    public boolean isConnected(String agentId) {
        WebSocketSession session = agentSessions.get(agentId);
        return session != null && session.isOpen();
    }

    public void sendMessage(String agentId, String message) throws IOException {
        WebSocketSession session = agentSessions.get(agentId);
        if (session == null || !session.isOpen()) {
            throw new IOException("Agent not connected: " + agentId);
        }
        session.sendMessage(new org.springframework.web.socket.TextMessage(message));
    }
}
