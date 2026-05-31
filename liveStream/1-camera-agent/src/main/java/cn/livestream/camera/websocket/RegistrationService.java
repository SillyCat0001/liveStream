package cn.livestream.camera.websocket;

import cn.livestream.camera.config.CameraConfig;
import cn.livestream.camera.model.Capabilities;
import cn.livestream.camera.websocket.model.RegisterMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RegistrationService {
    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private final CameraConfig config;
    private final WebSocketClient webSocketClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RegistrationService(CameraConfig config, WebSocketClient webSocketClient) {
        this.config = config;
        this.webSocketClient = webSocketClient;
    }

    public void register() {
        RegisterMessage message = new RegisterMessage();
        message.setType("REGISTER");
        message.setAgentId(config.getSn());
        message.setDeviceInfo(config.getDeviceName());
        message.setProtocolVersion("1.0");

        Capabilities capabilities = new Capabilities();
        capabilities.setProtocols(new String[]{"RTMP"});
        capabilities.setMaxResolution(config.getWidth() + "x" + config.getHeight());
        capabilities.setMaxFps(config.getFps());
        capabilities.setCodecs(new String[]{config.getCodec()});
        message.setCapabilities(capabilities);

        try {
            String json = objectMapper.writeValueAsString(message);
            webSocketClient.sendMessage(json);
            log.info("Device registered: {}", config.getSn());
        } catch (Exception e) {
            log.error("Failed to register device", e);
        }
    }
}