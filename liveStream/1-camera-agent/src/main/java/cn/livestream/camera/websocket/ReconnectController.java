package cn.livestream.camera.websocket;

import cn.livestream.camera.pusher.StreamPusher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ReconnectController implements ReconnectCallback {
    private static final Logger log = LoggerFactory.getLogger(ReconnectController.class);

    private final StreamPusher pusher;

    public ReconnectController(StreamPusher pusher) {
        this.pusher = pusher;
    }

    @Override
    public void onReconnectFailed(int attempts) {
        log.warn("WebSocket reconnect failed after {} attempts, stopping stream", attempts);
        try {
            pusher.stop();
        } catch (Exception e) {
            log.error("Failed to stop stream on reconnect failure", e);
        }
    }

    @Override
    public void onReconnectSuccess() {
        log.info("WebSocket reconnected successfully");
    }
}