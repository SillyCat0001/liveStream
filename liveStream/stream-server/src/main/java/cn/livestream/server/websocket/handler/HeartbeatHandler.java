package cn.livestream.server.websocket.handler;

import cn.livestream.server.model.ws.HeartbeatMessage;
import cn.livestream.server.service.AgentRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class HeartbeatHandler {
    private static final Logger log = LoggerFactory.getLogger(HeartbeatHandler.class);

    private final AgentRegistry registry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HeartbeatHandler(AgentRegistry registry) {
        this.registry = registry;
    }

    public void handle(WebSocketSession session, String payload) throws Exception {
        HeartbeatMessage msg = objectMapper.readValue(payload, HeartbeatMessage.class);
        registry.updateHeartbeat(msg.getAgentId());
        log.debug("Heartbeat received from agent: {}", msg.getAgentId());
    }
}