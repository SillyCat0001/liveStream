package cn.livestream.gateway.model;

import java.time.Instant;

public class ChannelInfo {
    private String channelId;
    private String streamKey;
    private String playbackUrlHls;
    private String playbackUrlWebRTC;
    private ChannelState state;
    private Instant createdAt;

    public ChannelInfo() {
        this.createdAt = Instant.now();
        this.state = ChannelState.STOPPED;
    }

    // getters and setters
    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }
    public String getStreamKey() { return streamKey; }
    public void setStreamKey(String streamKey) { this.streamKey = streamKey; }
    public String getPlaybackUrlHls() { return playbackUrlHls; }
    public void setPlaybackUrlHls(String playbackUrlHls) { this.playbackUrlHls = playbackUrlHls; }
    public String getPlaybackUrlWebRTC() { return playbackUrlWebRTC; }
    public void setPlaybackUrlWebRTC(String playbackUrlWebRTC) { this.playbackUrlWebRTC = playbackUrlWebRTC; }
    public ChannelState getState() { return state; }
    public void setState(ChannelState state) { this.state = state; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}