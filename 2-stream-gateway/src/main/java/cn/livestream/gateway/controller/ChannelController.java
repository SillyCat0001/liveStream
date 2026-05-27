package cn.livestream.gateway.controller;

import cn.livestream.gateway.model.ChannelInfo;
import cn.livestream.gateway.service.StreamGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/channels")
public class ChannelController {

    @Autowired
    private StreamGateway gateway;

    @PostMapping
    public Map<String, Object> createChannel() {
        try {
            ChannelInfo info = gateway.createChannel();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("channel", info);
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    @GetMapping("/{channelId}")
    public Map<String, Object> getChannel(@PathVariable String channelId) {
        ChannelInfo info = gateway.getChannel(channelId);
        Map<String, Object> result = new HashMap<>();
        if (info != null) {
            result.put("success", true);
            result.put("channel", info);
        } else {
            result.put("success", false);
            result.put("error", "Channel not found");
        }
        return result;
    }

    @PostMapping("/{channelId}/stop")
    public Map<String, Object> stopChannel(@PathVariable String channelId) {
        gateway.stopChannel(channelId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Channel stopped");
        return result;
    }

    @GetMapping("/{channelId}/playback")
    public Map<String, Object> getPlaybackUrl(
            @PathVariable String channelId,
            @RequestParam(defaultValue = "hls") String protocol) {
        try {
            String url = gateway.getPlaybackUrl(channelId, protocol);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("url", url);
            result.put("protocol", protocol);
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    @PostMapping("/{channelId}/failover")
    public Map<String, Object> failover(@PathVariable String channelId) {
        boolean switched = gateway.switchToBackup(channelId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", switched);
        result.put("message", switched ? "Switched to backup" : "Already on backup or not found");
        return result;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("healthy", gateway.isHealthy());
        return result;
    }
}