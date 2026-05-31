package cn.livestream.server.websocket.handler;

import cn.livestream.server.model.agent.AgentInfo;
import cn.livestream.server.model.agent.AgentStatus;
import cn.livestream.server.model.ws.RegisterMessage;
import cn.livestream.server.service.AgentConnectionManager;
import cn.livestream.server.service.AgentRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class RegisterHandler {
    private static final Logger log = LoggerFactory.getLogger(RegisterHandler.class);

    private final AgentRegistry registry;
    private final AgentConnectionManager connectionManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RegisterHandler(AgentRegistry registry, AgentConnectionManager connectionManager) {
        this.registry = registry;
        this.connectionManager = connectionManager;
    }

    public void handle(WebSocketSession session, String payload) throws Exception {
        RegisterMessage msg = objectMapper.readValue(payload, RegisterMessage.class);
        String agentId = msg.getAgentId();

        AgentInfo info = new AgentInfo();
        info.setAgentId(agentId);
        info.setDeviceName(msg.getDeviceInfo());
        info.setCapabilities(msg.getCapabilities());
        info.setStatus(AgentStatus.ONLINE);

        registry.register(info);
        connectionManager.register(agentId, session);
        log.info("Agent registered: {}", agentId);
    }
}