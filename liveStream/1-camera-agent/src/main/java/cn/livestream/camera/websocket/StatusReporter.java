package cn.livestream.camera.websocket;

import cn.livestream.camera.config.CameraConfig;
import cn.livestream.camera.pusher.RTMPPusher;
import cn.livestream.camera.websocket.model.StatusReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class StatusReporter {
    private static final Logger log = LoggerFactory.getLogger(StatusReporter.class);

    private final CameraConfig config;
    private final RTMPPusher pusher;
    private final WebSocketClient webSocketClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile boolean enabled = false;

    public StatusReporter(CameraConfig config, RTMPPusher pusher, WebSocketClient webSocketClient) {
        this.config = config;
        this.pusher = pusher;
        this.webSocketClient = webSocketClient;
    }

    public void start() {
        enabled = true;
        log.info("Status reporter started");
    }

    public void stop() {
        enabled = false;
        log.info("Status reporter stopped");
    }

    @Scheduled(fixedDelayString = "${camera.server.status-report-interval:5000}")
    public void reportStatus() {
        if (!enabled || !webSocketClient.isConnected()) {
            return;
        }

        StatusReport report = new StatusReport();
        report.setType("STATUS_REPORT");
        report.setAgentId(config.getSn());

        if (pusher.isRunning()) {
            report.setStatus("STREAMING");
            report.getStats().setFps(config.getFps());
            report.getStats().setBitrate(config.getVideoBitrate());
            report.getStats().setLatencyMs(100);
        } else {
            report.setStatus("ONLINE");
        }

        try {
            String json = objectMapper.writeValueAsString(report);
            webSocketClient.sendMessage(json);
        } catch (Exception e) {
            log.error("Failed to send status report", e);
        }
    }
}