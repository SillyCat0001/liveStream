package cn.livestream.camera.websocket;

import cn.livestream.camera.pusher.StreamPusher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ReconnectController implements ReconnectCallback {
    private static final Logger log = LoggerFactory.getLogger(ReconnectController.class);

    private final StreamPusher pusher;
    private final RegistrationService registrationService;
    private final StatusReporter statusReporter;

    public ReconnectController(StreamPusher pusher, RegistrationService registrationService,
                               StatusReporter statusReporter) {
        this.pusher = pusher;
        this.registrationService = registrationService;
        this.statusReporter = statusReporter;
    }

    @Override
    public void onReconnectFailed(int attempts) {
        log.warn("WebSocket reconnect failed after {} attempts, stopping stream", attempts);
        statusReporter.stop();
        try {
            pusher.stop();
        } catch (Exception e) {
            log.error("Failed to stop stream on reconnect failure", e);
        }
    }

    @Override
    public void onReconnectSuccess() {
        log.info("WebSocket connected, registering device");
        registrationService.register();
        statusReporter.start();
    }
}