package cn.livestream.camera.health;

import cn.livestream.camera.config.CameraConfig;
import cn.livestream.camera.ffmpeg.FFmpegWrapper;
import cn.livestream.camera.pusher.StreamPusher;
import cn.livestream.camera.websocket.StatusReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HealthChecker {
    private static final Logger log = LoggerFactory.getLogger(HealthChecker.class);

    private final FFmpegWrapper ffmpegWrapper;
    private final StreamPusher pusher;
    private final StatusReporter statusReporter;
    private final CameraConfig config;

    private int unhealthyCount = 0;
    private static final int UNHEALTHY_THRESHOLD = 3;

    public HealthChecker(FFmpegWrapper ffmpegWrapper, StreamPusher pusher,
                         StatusReporter statusReporter, CameraConfig config) {
        this.ffmpegWrapper = ffmpegWrapper;
        this.pusher = pusher;
        this.statusReporter = statusReporter;
        this.config = config;
    }

    @Scheduled(fixedDelay = 5000)
    public void checkHealth() {
        boolean expectedRunning = pusher.isRunning();
        boolean actuallyRunning = ffmpegWrapper.isRunning();

        if (expectedRunning && !actuallyRunning) {
            unhealthyCount++;
            log.warn("FFmpeg process dead while streaming expected, unhealthy count: {}/{}",
                    unhealthyCount, UNHEALTHY_THRESHOLD);

            if (unhealthyCount >= UNHEALTHY_THRESHOLD) {
                log.error("Unhealthy threshold exceeded, attempting to restart stream");
                pusher.stop();
                unhealthyCount = 0;
                try {
                    pusher.start(config);
                    log.info("Stream restarted successfully");
                } catch (Exception e) {
                    log.error("Failed to restart stream", e);
                    statusReporter.reportStatus();
                }
            }
        } else {
            unhealthyCount = 0;
            log.debug("Health check OK");
        }
    }

    public void reset() {
        unhealthyCount = 0;
    }

    public boolean isHealthy() {
        return ffmpegWrapper.isRunning();
    }

    public int getUnhealthyCount() {
        return unhealthyCount;
    }
}