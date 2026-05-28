package cn.livestream.server.model.stream;

import java.util.List;
import java.util.Map;

public class StreamInfo {
    private String channelId;
    private String agentId;
    private String status;
    private String rtmpUrl;
    private String streamKey;
    private List<String> protocols;
    private Map<String, String> playUrls;

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRtmpUrl() {
        return rtmpUrl;
    }

    public void setRtmpUrl(String rtmpUrl) {
        this.rtmpUrl = rtmpUrl;
    }

    public String getStreamKey() {
        return streamKey;
    }

    public void setStreamKey(String streamKey) {
        this.streamKey = streamKey;
    }

    public List<String> getProtocols() {
        return protocols;
    }

    public void setProtocols(List<String> protocols) {
        this.protocols = protocols;
    }

    public Map<String, String> getPlayUrls() {
        return playUrls;
    }

    public void setPlayUrls(Map<String, String> playUrls) {
        this.playUrls = playUrls;
    }
}