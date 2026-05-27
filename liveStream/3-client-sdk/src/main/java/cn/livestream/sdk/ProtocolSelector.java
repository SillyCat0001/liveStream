package cn.livestream.sdk;

import cn.livestream.sdk.protocol.PlayerProtocol;
import cn.livestream.sdk.protocol.HLSPlayer;
import cn.livestream.sdk.protocol.HTTPFLVPlayer;
import cn.livestream.sdk.protocol.WebRTCPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ProtocolSelector {
    private static final Logger log = LoggerFactory.getLogger(ProtocolSelector.class);

    private final Map<String, Supplier<PlayerProtocol>> protocolFactories = new HashMap<>();

    public ProtocolSelector() {
        registerProtocol("hls", HLSPlayer::new);
        registerProtocol("httpflv", HTTPFLVPlayer::new);
        registerProtocol("webrtc", WebRTCPlayer::new);
    }

    public void registerProtocol(String name, Supplier<PlayerProtocol> factory) {
        protocolFactories.put(name.toLowerCase(), factory);
        log.info("Registered protocol: {}", name);
    }

    public PlayerProtocol selectProtocol(PlayerConfig config) {
        String preferred = config.getPreferredProtocol().toLowerCase();

        if ("auto".equals(preferred)) {
            return selectBestProtocol();
        }

        PlayerProtocol protocol = protocolFactories.get(preferred).get();
        protocol.initialize(config);
        return protocol;
    }

    public PlayerProtocol selectBestProtocol() {
        log.info("Auto-selecting protocol: HLS (default for compatibility)");
        PlayerProtocol protocol = new HLSPlayer();
        protocol.initialize(new PlayerConfig());
        return protocol;
    }

    public PlayerProtocol createProtocol(String name, PlayerConfig config) {
        PlayerProtocol protocol = protocolFactories.get(name.toLowerCase()).get();
        protocol.initialize(config);
        return protocol;
    }
}