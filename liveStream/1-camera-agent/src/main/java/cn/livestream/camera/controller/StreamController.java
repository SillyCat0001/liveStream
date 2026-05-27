package cn.livestream.camera.controller;

import cn.livestream.camera.config.CameraConfig;
import cn.livestream.camera.pusher.RTMPPusher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stream")
public class StreamController {

    @Autowired
    private RTMPPusher pusher;

    @Autowired
    private CameraConfig config;

    @PostMapping("/start")
    public Map<String, Object> start() {
        try {
            pusher.start(config);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Stream started");
            result.put("url", config.getRtmpUrl() + "/" + config.getStreamKey());
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    @PostMapping("/stop")
    public Map<String, Object> stop() {
        pusher.stop();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Stream stopped");
        return result;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> result = new HashMap<>();
        result.put("running", pusher.isRunning());
        result.put("config", Map.of(
            "rtmpUrl", config.getRtmpUrl(),
            "streamKey", config.getStreamKey(),
            "bitrate", config.getVideoBitrate(),
            "fps", config.getFps()
        ));
        return result;
    }

    @PostMapping("/bitrate")
    public Map<String, Object> updateBitrate(@RequestParam int bitrate) {
        pusher.updateBitrate(bitrate);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("bitrate", bitrate);
        return result;
    }
}