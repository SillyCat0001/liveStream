package cn.livestream.server.service;

import cn.livestream.server.model.stream.StreamInfo;
import cn.livestream.server.model.ws.CommandResponse;
import cn.livestream.server.model.ws.StartStreamCommand;
import cn.livestream.server.model.ws.StopStreamCommand;
import cn.livestream.server.websocket.FrontendWebSocketServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class StreamCoordinator {
    private static final Logger log = LoggerFactory.getLogger(StreamCoordinator.class);
    private static final String HEARTBEAT_KEY_PREFIX = "STREAM:HEARTBEAT:";
    private static final long HEARTBEAT_TTL_SECONDS = 30;

    private final AgentConnectionManager connectionManager;
    private final StringRedisTemplate redisTemplate;
    private final FrontendWebSocketServer frontendWebSocketServer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, StreamInfo> activeStreams = new ConcurrentHashMap<>();

    public StreamCoordinator(AgentConnectionManager connectionManager,
                              StringRedisTemplate redisTemplate,
                              FrontendWebSocketServer frontendWebSocketServer) {
        this.connectionManager = connectionManager;
        this.redisTemplate = redisTemplate;
        this.frontendWebSocketServer = frontendWebSocketServer;
    }

    public StreamInfo startStream(String agentId, String rtmpUrl, String streamKey) throws Exception {
        String channelId = UUID.randomUUID().toString();

        StartStreamCommand cmd = new StartStreamCommand();
        cmd.setType("START_STREAM");
        cmd.setRtmpUrl(rtmpUrl);
        cmd.setStreamKey(streamKey);
        cmd.setConfig(new StartStreamCommand.StreamConfig());

        String message = objectMapper.writeValueAsString(cmd);
        connectionManager.sendMessage(agentId, message);

        StreamInfo info = new StreamInfo();
        info.setChannelId(channelId);
        info.setAgentId(agentId);
        info.setStatus("PENDING");
        info.setRtmpUrl(rtmpUrl);
        info.setStreamKey(streamKey);
        info.setPlayUrls(buildPlayUrls(rtmpUrl, streamKey));

        activeStreams.put(agentId, info);
        renewHeartbeat(agentId);
        log.info("Start stream command sent to agent: {}", agentId);
        return info;
    }

    public void stopStream(String agentId) throws Exception {
        String heartbeatKey = HEARTBEAT_KEY_PREFIX + agentId;
        redisTemplate.delete(heartbeatKey);

        StopStreamCommand cmd = new StopStreamCommand();
        cmd.setType("STOP_STREAM");

        String message = objectMapper.writeValueAsString(cmd);
        connectionManager.sendMessage(agentId, message);

        activeStreams.remove(agentId);
        log.info("Stop stream command sent to agent: {}", agentId);
    }

    public void onCommandResponse(CommandResponse response) {
        log.info("Agent responded to {}: success={}, message={}",
            response.getOriginalType(), response.isSuccess(), response.getMessage());
    }

    public StreamInfo getStreamInfo(String agentId) {
        return activeStreams.get(agentId);
    }

    public void renewHeartbeat(String agentId) {
        String key = HEARTBEAT_KEY_PREFIX + agentId;
        redisTemplate.opsForValue().set(key, agentId, HEARTBEAT_TTL_SECONDS, TimeUnit.SECONDS);
    }

    public void onHeartbeatExpired(String agentId) {
        try {
            stopStream(agentId);
            frontendWebSocketServer.sendStreamStopped(agentId, "HEARTBEAT_TIMEOUT");
        } catch (Exception e) {
            log.error("Failed to handle heartbeat expiry for agent: {}", agentId, e);
        }
    }

    private Map<String, String> buildPlayUrls(String rtmpUrl, String streamKey) {
        Map<String, String> urls = new ConcurrentHashMap<>();
        urls.put("rtmp", rtmpUrl + "/" + streamKey);
        urls.put("hls", "http://localhost:8080/live/" + streamKey + ".m3u8");
        urls.put("httpflv", "http://localhost:8080/live/" + streamKey + ".flv");
        return urls;
    }
}