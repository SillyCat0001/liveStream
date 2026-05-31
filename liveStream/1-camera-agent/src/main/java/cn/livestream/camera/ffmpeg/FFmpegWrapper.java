package cn.livestream.camera.ffmpeg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import cn.livestream.camera.config.CameraConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FFmpegWrapper {
    private static final Logger log = LoggerFactory.getLogger(FFmpegWrapper.class);

    @Autowired
    private CameraConfig config;

    private Process process;
    private volatile boolean running = false;

    private String findVideoDevice() throws IOException {
        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg");
        cmd.add("-list_devices");
        cmd.add("true");
        cmd.add("-f");
        cmd.add("dshow");
        cmd.add("-i");
        cmd.add("dummy");

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        Pattern videoPattern = Pattern.compile("\"([^\"]+)\"\\s*\\(video\\)");
        Pattern altPattern = Pattern.compile("Alternative name\\s*:\\s*(.+)");
        String lastDevice = null;
        boolean nextIsVideoDevice = false;
        for (String line : output.toString().split("\n")) {
            String trimmed = line.trim();
            Matcher videoMatcher = videoPattern.matcher(trimmed);
            if (videoMatcher.find()) {
                lastDevice = videoMatcher.group(1);
                nextIsVideoDevice = true;
            } else if (nextIsVideoDevice) {
                Matcher altMatcher = altPattern.matcher(trimmed);
                if (altMatcher.find()) {
                    lastDevice = altMatcher.group(1).trim();
                }
                break;
            }
        }

        if (lastDevice == null || lastDevice.isEmpty()) {
            throw new IOException("No video device found on Windows system");
        }

        log.info("Auto-detected video device: {}", lastDevice);
        return lastDevice;
    }

/**
 * Starts the FFmpeg process for video streaming
 * @throws IOException if an I/O error occurs when starting the process
 */
    public void start() throws IOException {
        // Check if FFmpeg is already running
        if (running) {
            log.warn("FFmpeg already running");
            return;
        }

        // Find and log the video device name
        String videoDevice = findVideoDevice();
        System.out.println("device name: " + videoDevice);

        // Build FFmpeg command with necessary parameters
        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg"); // Base command
        cmd.add("-f"); // Format option
        cmd.add("dshow"); // DirectShow input format for Windows
        cmd.add("-i"); // Input option
        cmd.add("video=" + videoDevice); // Video device specification
        cmd.add("-c:v"); // Video codec option
        cmd.add("libx264"); // H.264 video codec
        cmd.add("-preset"); // Encoding speed preset
        cmd.add("ultrafast"); // Fastest encoding preset
        cmd.add("-tune"); // Optimization option
        cmd.add("zerolatency");
        cmd.add("-crf");
        cmd.add("23");
        cmd.add("-c:a");
        cmd.add("aac");
        cmd.add("-b:a");
        cmd.add(config.getAudioBitrate() + "k");
        String streamUrl = config.getRtmpUrl() + "/" + config.getStreamKey();
        cmd.add("-f");
        cmd.add("flv");
        cmd.add(streamUrl);

        log.info("Starting FFmpeg: {}", String.join(" ", cmd));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        process = pb.start();
        running = true;

        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while (running && reader.read() != -1) {
                    System.out.println(reader.readLine());
                }
            } catch (IOException e) {
                if (running) log.error("FFmpeg output read error", e);
            }
        }).start();
    }

    public void stop() {
        if (!running) return;
        running = false;
        if (process != null) {
            long pid = process.pid();
            try {
                Runtime.getRuntime().exec(
                        "taskkill /PID " + pid + " /T /F"
                );
                process.waitFor();
            } catch (Exception e) {
                Thread.currentThread().interrupt();
                log.error(e.getMessage());
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