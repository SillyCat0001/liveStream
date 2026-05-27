package cn.livestream.camera;

import cn.livestream.camera.websocket.RegistrationService;
import cn.livestream.camera.websocket.StandardWebSocketClient;
import cn.livestream.camera.websocket.StatusReporter;
import cn.livestream.camera.websocket.StreamMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.client.WebSocketConnectionManager;

@SpringBootApplication
@EnableScheduling
public class CameraApplication {
    private static final Logger log = LoggerFactory.getLogger(CameraApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(CameraApplication.class, args);
    }

    @Bean
    public WebSocketConnectionManager webSocketConnectionManager(
            StandardWebSocketClient client,
            StreamMessageHandler messageHandler,
            RegistrationService registrationService,
            StatusReporter statusReporter) {

        client.onMessage(messageHandler::handleMessage);
        client.connect();
        registrationService.register();
        statusReporter.start();

        log.info("Camera agent started with WebSocket remote control");

        return null;
    }
}