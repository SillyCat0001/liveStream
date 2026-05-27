package cn.livestream.gateway.service;

import cn.livestream.gateway.model.ChannelInfo;
import cn.livestream.gateway.model.ChannelState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
public class SRSBridge {
    private static final Logger log = LoggerFactory.getLogger(SRSBridge.class);

    @Value("${srs.url:http://localhost:8082}")
    private String srsUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public ChannelInfo createChannel() {
        String channelId = "srs-" + UUID.randomUUID().toString().substring(0, 8);
        String streamKey = "stream-" + System.currentTimeMillis();

        ChannelInfo info = new ChannelInfo();
        info.setChannelId(channelId);
        info.setStreamKey(streamKey);
        info.setPlaybackUrlHls(String.format("%s/live/%s.m3u8", srsUrl, streamKey));
        info.setPlaybackUrlWebRTC(String.format("%s/live/%s", srsUrl, streamKey));
        info.setState(ChannelState.ACTIVE);

        log.info("Created SRS channel: {}", channelId);
        return info;
    }

    public ChannelInfo getChannel(String channelId) {
        ChannelInfo info = new ChannelInfo();
        info.setChannelId(channelId);
        info.setState(ChannelState.ACTIVE);
        return info;
    }

    public void stopChannel(String channelId) {
        log.info("Stopping SRS channel (no-op for SRS): {}", channelId);
    }

    public String getIngestUrl(String streamKey) {
        return String.format("%s/live/%s", srsUrl, streamKey);
    }

    public boolean isHealthy() {
        try {
            String healthUrl = srsUrl + "/api/v1/features";
            restTemplate.getForObject(healthUrl, Object.class);
            return true;
        } catch (Exception e) {
            log.error("SRS health check failed", e);
            return false;
        }
    }
}