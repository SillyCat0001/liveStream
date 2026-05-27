package cn.livestream.sdk.protocol;

import cn.livestream.sdk.PlayerConfig;
import cn.livestream.sdk.PlayerStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class HLSPlayer implements PlayerProtocol {
    private static final Logger log = LoggerFactory.getLogger(HLSPlayer.class);

    private String name = "HLS";
    private PlayerConfig config;
    private volatile boolean playing = false;
    private PlayerStats stats = new PlayerStats();

    @Override
    public String getName() { return name; }

    @Override
    public void initialize(PlayerConfig config) {
        this.config = config;
        log.info("HLSPlayer initialized with URL: {}", config.getStreamUrl());
    }

    @Override
    public void play() {
        playing = true;
        log.info("HLSPlayer starting playback");
        stats.setVideoBitrate(2000);
        stats.setFps(30.0);
        stats.setLatencyMs(3000);
    }

    @Override
    public void stop() {
        playing = false;
        log.info("HLSPlayer stopped");
    }

    @Override
    public void pause() { playing = false; }

    @Override
    public void resume() { playing = true; }

    @Override
    public boolean isPlaying() { return playing; }

    @Override
    public PlayerStats getStats() { return stats; }

    @Override
    public void addStatsListener(Consumer<PlayerStats> listener) {}
}