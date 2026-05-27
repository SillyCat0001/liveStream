package cn.livestream.gateway.service;

import cn.livestream.gateway.model.ChannelInfo;
import cn.livestream.gateway.model.ChannelState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ivs.IvsClient;
import software.amazon.awssdk.services.ivs.model.*;

import jakarta.annotation.PostConstruct;
import java.net.URI;

@Service
public class IVSBridge {
    private static final Logger log = LoggerFactory.getLogger(IVSBridge.class);

    @Value("${aws.ivs.region:us-east-1}")
    private String region;

    private IvsClient ivsClient;

    @PostConstruct
    public void init() {
        ivsClient = IvsClient.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create("https://ivs." + region + ".amazonaws.com"))
                .build();
        log.info("IVS client initialized for region: {}", region);
    }

    public ChannelInfo createChannel() {
        CreateChannelRequest request = CreateChannelRequest.builder()
                .name("camera-" + System.currentTimeMillis())
                .type(ChannelType.SIMPLE)
                .build();

        CreateChannelResponse response = ivsClient.createChannel(request);
        Channel channel = response.channel();

        ChannelInfo info = new ChannelInfo();
        info.setChannelId(channel.arn());
        info.setStreamKey(response.streamKey().value());
        info.setPlaybackUrlHls(channel.playbackUrl());
        info.setState(ChannelState.ACTIVE);

        log.info("Created IVS channel: {}", info.getChannelId());
        return info;
    }

    public ChannelInfo getChannel(String channelArn) {
        GetChannelRequest request = GetChannelRequest.builder()
                .arn(channelArn)
                .build();

        Channel channel = ivsClient.getChannel(request).channel();
        ChannelInfo info = new ChannelInfo();
        info.setChannelId(channel.arn());
        info.setPlaybackUrlHls(channel.playbackUrl());
        info.setState(ChannelState.ACTIVE);

        return info;
    }

    public void stopChannel(String channelArn) {
        try {
            StopChannelRequest request = StopChannelRequest.builder()
                    .arn(channelArn)
                    .build();
            ivsClient.stopChannel(request);
            log.info("Stopped IVS channel: {}", channelArn);
        } catch (Exception e) {
            log.error("Failed to stop channel: {}", channelArn, e);
        }
    }

    public boolean isHealthy() {
        try {
            ivsClient.listChannels(ListChannelsRequest.builder().maxResults(1).build());
            return true;
        } catch (Exception e) {
            log.error("IVS health check failed", e);
            return false;
        }
    }
}