package cn.livestream.server.controller;

import cn.livestream.server.model.stream.StartStreamRequest;
import cn.livestream.server.model.stream.StartStreamResponse;
import cn.livestream.server.model.stream.StreamInfo;
import cn.livestream.server.service.AgentRegistry;
import cn.livestream.server.service.StreamCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/stream")
public class StreamController {
    private static final Logger log = LoggerFactory.getLogger(StreamController.class);
    private static final String HEARTBEAT_KEY_PREFIX = "STREAM:HEARTBEAT:";
    private static final long HEARTBEAT_TTL_SECONDS = 30;

    private final StreamCoordinator streamCoordinator;
    private final AgentRegistry registry;
    private final StringRedisTemplate redisTemplate;

    public StreamController(StreamCoordinator streamCoordinator, AgentRegistry registry, StringRedisTemplate redisTemplate) {
        this.streamCoordinator = streamCoordinator;
        this.registry = registry;
        this.redisTemplate = redisTemplate;
    }

    @PostMapping("/start")
    public ResponseEntity<StartStreamResponse> startStream(@RequestBody StartStreamRequest request) {
        String agentId = request.getAgentId();
        log.info("Start stream request for agent: {}", agentId);

        if (!registry.isAgentOnline(agentId)) {
            StartStreamResponse resp = new StartStreamResponse();
            resp.setSuccess(false);
            resp.setMessage("Agent not online: " + agentId);
            return ResponseEntity.status(503).body(resp);
        }

        try {
            StreamInfo info = streamCoordinator.startStream(
                agentId,
                "rtmp://localhost:1935/livestream", agentId
            );
            StartStreamResponse resp = new StartStreamResponse();
            resp.setSuccess(true);
            resp.setData(info);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("Failed to start stream for agent: {}", agentId, e);
            StartStreamResponse resp = new StartStreamResponse();
            resp.setSuccess(false);
            resp.setMessage(e.getMessage());
            return ResponseEntity.status(500).body(resp);
        }
    }

    @DeleteMapping("/stop")
    public ResponseEntity<StartStreamResponse> stopStream(@RequestParam String agentId) {
        log.info("Stop stream request for agent: {}", agentId);

        try {
            streamCoordinator.stopStream(agentId);
            StartStreamResponse resp = new StartStreamResponse();
            resp.setSuccess(true);
            resp.setMessage("Stream stopped");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("Failed to stop stream for agent: {}", agentId, e);
            StartStreamResponse resp = new StartStreamResponse();
            resp.setSuccess(false);
            resp.setMessage(e.getMessage());
            return ResponseEntity.status(500).body(resp);
        }
    }

    @GetMapping("/status/{agentId}")
    public ResponseEntity<StreamInfo> getStatus(@PathVariable String agentId) {
        StreamInfo info = streamCoordinator.getStreamInfo(agentId);
        if (info == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(info);
    }

    @PutMapping("/heartbeat")
    public ResponseEntity<Map<String, Boolean>> heartbeat(@RequestParam String agentId) {
        try {
            String key = HEARTBEAT_KEY_PREFIX + agentId;
            if(redisTemplate.opsForValue().get(key) == null) {
                redisTemplate.opsForValue().set(key, agentId, HEARTBEAT_TTL_SECONDS, TimeUnit.SECONDS);
            }else {
                redisTemplate.opsForValue().getAndExpire(key, HEARTBEAT_TTL_SECONDS, TimeUnit.SECONDS);
            }
            Map<String, Boolean> resp = Map.of("success", true);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("Failed to renew heartbeat for agent: {}", agentId, e);
            Map<String, Boolean> resp = Map.of("success", false);
            return ResponseEntity.status(500).body(resp);
        }
    }
}