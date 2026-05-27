package cn.livestream.camera.pusher;

import cn.livestream.camera.config.CameraConfig;
import cn.livestream.camera.ffmpeg.FFmpegWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RTMPPusher implements StreamPusher {
    private static final Logger log = LoggerFactory.getLogger(RTMPPusher.class);

    @Autowired
    private FFmpegWrapper ffmpegWrapper;

    private volatile boolean running = false;

    @Override
    public void start(CameraConfig config) {
        if (running) {
            log.warn("Pusher already running");
            return;
        }

        try {
            ffmpegWrapper.start();
            running = true;
            log.info("RTMP pusher started, stream: {}/{}", config.getRtmpUrl(), config.getStreamKey());
        } catch (IOException e) {
            log.error("Failed to start RTMP pusher", e);
            throw new RuntimeException("Failed to start pusher", e);
        }
    }

    @Override
    public void stop() {
        if (!running) return;
        running = false;
        ffmpegWrapper.stop();
        log.info("RTMP pusher stopped");
    }

    @Override
    public boolean isRunning() {
        return running && ffmpegWrapper.isRunning();
    }

    @Override
    public void updateBitrate(int bitrate) {
        ffmpegWrapper.updateBitrate(bitrate);
    }
}
