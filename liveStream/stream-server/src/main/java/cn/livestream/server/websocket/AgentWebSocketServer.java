package cn.livestream.server.websocket;

import cn.livestream.server.service.AgentConnectionManager;
import cn.livestream.server.service.AgentRegistry;
import cn.livestream.server.websocket.handler.RegisterHandler;
import cn.livestream.server.websocket.handler.HeartbeatHandler;
import cn.livestream.server.websocket.handler.CommandResponseHandler;
import cn.livestream.server.websocket.handler.StatusReportHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class AgentWebSocketServer extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(AgentWebSocketServer.class);

    private final AgentConnectionManager connectionManager;
    private final AgentRegistry registry;
    private final RegisterHandler registerHandler;
    private final HeartbeatHandler heartbeatHandler;
    private final CommandResponseHandler commandResponseHandler;
    private final StatusReportHandler statusReportHandler;

    public AgentWebSocketServer(
            AgentConnectionManager connectionManager,
            AgentRegistry registry,
            RegisterHandler registerHandler,
            HeartbeatHandler heartbeatHandler,
            CommandResponseHandler commandResponseHandler,
            StatusReportHandler statusReportHandler) {
        this.connectionManager = connectionManager;
        this.registry = registry;
        this.registerHandler = registerHandler;
        this.heartbeatHandler = heartbeatHandler;
        this.commandResponseHandler = commandResponseHandler;
        this.statusReportHandler = statusReportHandler;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("Agent connected: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            System.out.println(message.getPayload());
            String payload = message.getPayload();
            var objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = objectMapper.readTree(payload);
            var typeNode = node.get("type");
            if (typeNode == null) {
                log.warn("Message missing 'type' field: {}", payload);
                return;
            }
            String type = typeNode.asText();

            switch (type) {
                case "REGISTER" -> registerHandler.handle(session, payload);
                case "HEARTBEAT" -> heartbeatHandler.handle(session, payload);
                case "COMMAND_RESPONSE" -> commandResponseHandler.handle(session, payload);
                case "STATUS_REPORT" -> statusReportHandler.handle(session, payload);
                default -> log.warn("Unknown message type: {}", type);
            }
        } catch (Exception e) {
            log.error("Failed to handle message", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String agentId = connectionManager.getAgentIdBySession(session.getId());
        if (agentId != null) {
            connectionManager.unregister(agentId);
            registry.setStatus(agentId, cn.livestream.server.model.agent.AgentStatus.OFFLINE);
            log.info("Agent {} disconnected (session {})", agentId, session.getId());
        } else {
            log.info("Agent disconnected (session {}, agentId=unknown)", session.getId());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("Transport error for session {}", session.getId(), exception);
    }
}