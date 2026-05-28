package cn.livestream.server.controller;

import cn.livestream.server.model.stream.StartStreamRequest;
import cn.livestream.server.model.stream.StartStreamResponse;
import cn.livestream.server.model.stream.StreamInfo;
import cn.livestream.server.service.AgentRegistry;
import cn.livestream.server.service.StreamCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stream")
public class StreamController {
    private static final Logger log = LoggerFactory.getLogger(StreamController.class);

    private final StreamCoordinator streamCoordinator;
    private final AgentRegistry registry;

    public StreamController(StreamCoordinator streamCoordinator, AgentRegistry registry) {
        this.streamCoordinator = streamCoordinator;
        this.registry = registry;
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
                "rtmp://localhost/live",
                "stream-" + agentId
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
}