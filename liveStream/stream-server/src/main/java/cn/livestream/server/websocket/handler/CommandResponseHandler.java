package cn.livestream.server.websocket.handler;

import cn.livestream.server.model.ws.CommandResponse;
import cn.livestream.server.service.StreamCoordinator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class CommandResponseHandler {
    private static final Logger log = LoggerFactory.getLogger(CommandResponseHandler.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StreamCoordinator streamCoordinator;

    public CommandResponseHandler(StreamCoordinator streamCoordinator) {
        this.streamCoordinator = streamCoordinator;
    }

    public void handle(WebSocketSession session, String payload) throws Exception {
        CommandResponse msg = objectMapper.readValue(payload, CommandResponse.class);
        streamCoordinator.onCommandResponse(msg);
        log.info("Command response from agent: {}, success={}", msg.getOriginalType(), msg.isSuccess());
    }
}