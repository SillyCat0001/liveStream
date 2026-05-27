package cn.livestream.camera.ffmpeg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import cn.livestream.camera.config.CameraConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
@Component
public class FFmpegWrapper {
    private static final Logger log = LoggerFactory.getLogger(FFmpegWrapper.class);

    @Autowired
    private CameraConfig config;

    private Process process;
    private volatile boolean running = false;

    public void start() throws IOException {
        if (running) {
            log.warn("FFmpeg already running");
            return;
        }

        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg");
        cmd.add("-f");
        cmd.add("avfoundation");  // macOS capture, use dshow/vfwcap on Windows
        cmd.add("-i");
        cmd.add("0");  // default camera device
        cmd.add("-c:v");
        cmd.add("libx264");
        cmd.add("-preset");
        cmd.add("ultrafast");
        cmd.add("-tune");
        cmd.add("zerolatency");
        cmd.add("-b:v");
        cmd.add(config.getVideoBitrate() + "k");
        cmd.add("-r");
        cmd.add(String.valueOf(config.getFps()));
        cmd.add("-c:a");
        cmd.add("aac");
        cmd.add("-b:a");
        cmd.add(config.getAudioBitrate() + "k");
        cmd.add("-f");
        cmd.add("flv");
        cmd.add(config.getRtmpUrl() + "/" + config.getStreamKey());

        log.info("Starting FFmpeg: {}", String.join(" ", cmd));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        process = pb.start();
        running = true;

        // consume output in background thread
        new Thread(() -> {
            try (var reader = process.getInputStream()) {
                while (running && reader.read() != -1) {}
            } catch (IOException e) {
                if (running) log.error("FFmpeg output read error", e);
            }
        }).start();
    }

    public void stop() {
        if (!running) return;
        running = false;
        if (process != null) {
            process.destroy();
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        log.info("FFmpeg stopped");
    }

    public boolean isRunning() {
        return running && process != null && process.isAlive();
    }

    public void updateBitrate(int bitrate) {
        config.setVideoBitrate(bitrate);
        log.info("Bitrate update requested: {}", bitrate);
    }
}