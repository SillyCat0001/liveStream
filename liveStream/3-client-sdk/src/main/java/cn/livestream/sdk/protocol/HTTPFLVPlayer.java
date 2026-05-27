package cn.livestream.sdk.protocol;

import cn.livestream.sdk.PlayerConfig;
import cn.livestream.sdk.PlayerStats;

public class HTTPFLVPlayer implements PlayerProtocol {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HTTPFLVPlayer.class);

    private String name = "HTTP-FLV";
    private PlayerConfig config;
    private volatile boolean playing = false;
    private PlayerStats stats = new PlayerStats();

    @Override
    public String getName() { return name; }

    @Override
    public void initialize(PlayerConfig config) {
        this.config = config;
        log.info("HTTPFLVPlayer initialized with URL: {}", config.getStreamUrl());
    }

    @Override
    public void play() {
        playing = true;
        log.info("HTTPFLVPlayer starting playback");
        stats.setVideoBitrate(2000);
        stats.setFps(30.0);
        stats.setLatencyMs(1500);
    }

    @Override
    public void stop() { playing = false; }

    @Override
    public void pause() { playing = false; }

    @Override
    public void resume() { playing = true; }

    @Override
    public boolean isPlaying() { return playing; }

    @Override
    public PlayerStats getStats() { return stats; }

    @Override
    public void addStatsListener(java.util.function.Consumer<PlayerStats> listener) {}
}