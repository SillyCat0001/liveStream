package cn.livestream.camera.websocket;

import cn.livestream.camera.config.CameraConfig;
import cn.livestream.camera.health.HealthChecker;
import cn.livestream.camera.pusher.RTMPPusher;
import cn.livestream.camera.websocket.model.CommandResponse;
import cn.livestream.camera.websocket.model.StartStreamCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StreamMessageHandler {
    private static final Logger log = LoggerFactory.getLogger(StreamMessageHandler.class);

    private final CameraConfig config;
    private final RTMPPusher pusher;
    private final WebSocketClient webSocketClient;
    private final HealthChecker healthChecker;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StreamMessageHandler(CameraConfig config, RTMPPusher pusher, WebSocketClient webSocketClient,
                                HealthChecker healthChecker) {
        this.config = config;
        this.pusher = pusher;
        this.webSocketClient = webSocketClient;
        this.healthChecker = healthChecker;
    }

    public void handleMessage(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String type = node.get("type").asText();

            switch (type) {
                case "START_STREAM":
                    handleStartStream(objectMapper.readValue(message, StartStreamCommand.class));
                    break;
                case "STOP_STREAM":
                    handleStopStream();
                    break;
                default:
                    log.warn("Unknown message type: {}", type);
            }
        } catch (Exception e) {
            log.error("Failed to handle message", e);
        }
    }

    private void handleStartStream(StartStreamCommand command) {
        try {
            config.setRtmpUrl(command.getRtmpUrl());
            config.setStreamKey(command.getStreamKey());

            if (command.getConfig() != null) {
                config.setWidth(command.getConfig().getWidth());
                config.setHeight(command.getConfig().getHeight());
                config.setFps(command.getConfig().getFps());
                config.setVideoBitrate(command.getConfig().getBitrate());
            }

            pusher.start(config);
            healthChecker.reset();
            sendResponse("START_STREAM", true, "Stream started");

            log.info("Stream started: {}/{}", command.getRtmpUrl(), command.getStreamKey());
        } catch (Exception e) {
            log.error("Failed to start stream", e);
            sendResponse("START_STREAM", false, e.getMessage());
        }
    }

    private void handleStopStream() {
        try {
            pusher.stop();
            healthChecker.reset();
            sendResponse("STOP_STREAM", true, "Stream stopped");
            log.info("Stream stopped");
        } catch (Exception e) {
            log.error("Failed to stop stream", e);
            sendResponse("STOP_STREAM", false, e.getMessage());
        }
    }

    private void sendResponse(String originalType, boolean success, String message) {
        CommandResponse response = new CommandResponse();
        response.setType("COMMAND_RESPONSE");
        response.setOriginalType(originalType);
        response.setSuccess(success);
        response.setMessage(message);

        try {
            String json = objectMapper.writeValueAsString(response);
            webSocketClient.sendMessage(json);
        } catch (Exception e) {
            log.error("Failed to send response", e);
        }
    }
}