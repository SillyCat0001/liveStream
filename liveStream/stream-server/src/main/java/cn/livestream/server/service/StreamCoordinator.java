package cn.livestream.server.service;

import cn.livestream.server.model.stream.StreamInfo;
import cn.livestream.server.model.ws.CommandResponse;
import cn.livestream.server.model.ws.StartStreamCommand;
import cn.livestream.server.model.ws.StopStreamCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StreamCoordinator {
    private static final Logger log = LoggerFactory.getLogger(StreamCoordinator.class);

    private final AgentConnectionManager connectionManager;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, StreamInfo> activeStreams = new ConcurrentHashMap<>();

    public StreamCoordinator(AgentConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
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
        log.info("Start stream command sent to agent: {}", agentId);
        return info;
    }

    public void stopStream(String agentId) throws Exception {
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

    private Map<String, String> buildPlayUrls(String rtmpUrl, String streamKey) {
        Map<String, String> urls = new ConcurrentHashMap<>();
        urls.put("rtmp", rtmpUrl + "/" + streamKey);
        urls.put("hls", "http://localhost:8080/live/" + streamKey + ".m3u8");
        urls.put("httpflv", "http://localhost:8080/live/" + streamKey + ".flv");
        return urls;
    }
}