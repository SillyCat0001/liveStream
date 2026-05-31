package cn.livestream.camera.controller;

import cn.livestream.camera.health.HealthChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @Autowired
    private HealthChecker healthChecker;

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("healthy", healthChecker.isHealthy());
        result.put("unhealthyCount", healthChecker.getUnhealthyCount());
        return result;
    }
}