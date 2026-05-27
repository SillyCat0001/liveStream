package cn.livestream.gateway.service;

import cn.livestream.gateway.model.ChannelInfo;
import cn.livestream.gateway.model.ChannelState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StreamGateway {
    private static final Logger log = LoggerFactory.getLogger(StreamGateway.class);

    @Autowired
    private IVSBridge ivsBridge;

    @Autowired
    private SRSBridge srsBridge;

    private final Map<String, ChannelInfo> channels = new ConcurrentHashMap<>();
    private final Map<String, String> channelProvider = new ConcurrentHashMap<>();

    public ChannelInfo createChannel() {
        ChannelInfo info = ivsBridge.createChannel();
        channels.put(info.getChannelId(), info);
        channelProvider.put(info.getChannelId(), "ivs");
        log.info("Created channel via IVS: {}", info.getChannelId());
        return info;
    }

    public ChannelInfo getChannel(String channelId) {
        ChannelInfo info = channels.get(channelId);
        if (info == null) {
            String provider = channelProvider.get(channelId);
            if ("ivs".equals(provider)) {
                info = ivsBridge.getChannel(channelId);
            } else {
                info = srsBridge.getChannel(channelId);
            }
        }
        return info;
    }

    public void stopChannel(String channelId) {
        ChannelInfo info = channels.get(channelId);
        if (info != null) {
            String provider = channelProvider.get(channelId);
            if ("ivs".equals(provider)) {
                ivsBridge.stopChannel(channelId);
            } else {
                srsBridge.stopChannel(channelId);
            }
            info.setState(ChannelState.STOPPED);
            log.info("Stopped channel: {}", channelId);
        }
    }

    public String getPlaybackUrl(String channelId, String protocol) {
        ChannelInfo info = getChannel(channelId);
        if (info == null) {
            throw new IllegalArgumentException("Channel not found: " + channelId);
        }

        return switch (protocol.toLowerCase()) {
            case "hls" -> info.getPlaybackUrlHls();
            case "webrtc" -> info.getPlaybackUrlWebRTC();
            case "httpflv", "flv" -> info.getPlaybackUrlHls().replace(".m3u8", ".flv");
            default -> info.getPlaybackUrlHls();
        };
    }

    public boolean isHealthy() {
        return ivsBridge.isHealthy() || srsBridge.isHealthy();
    }

    public boolean switchToBackup(String channelId) {
        if (channelProvider.get(channelId).equals("ivs")) {
            log.info("Switching channel {} to SRS backup", channelId);
            channelProvider.put(channelId, "srs");
            return true;
        }
        return false;
    }
}