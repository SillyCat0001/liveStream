package cn.livestream.camera.health;

import cn.livestream.camera.ffmpeg.FFmpegWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HealthChecker {
    private static final Logger log = LoggerFactory.getLogger(HealthChecker.class);

    @Autowired
    private FFmpegWrapper ffmpegWrapper;

    private int reconnectCount = 0;
    private static final int RECONNECT_THRESHOLD = 10;

    @Scheduled(fixedDelay = 30000)
    public void checkHealth() {
        boolean healthy = ffmpegWrapper.isRunning();

        if (!healthy) {
            reconnectCount++;
            log.warn("Health check failed, reconnect attempt: {}", reconnectCount);

            if (reconnectCount > RECONNECT_THRESHOLD) {
                log.error("Reconnect threshold exceeded, manual intervention required");
            }
        } else {
            reconnectCount = 0;
            log.debug("Health check OK");
        }
    }

    public boolean isHealthy() {
        return ffmpegWrapper.isRunning();
    }

    public int getReconnectCount() {
        return reconnectCount;
    }
}