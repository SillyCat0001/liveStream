package cn.livestream.sdk.protocol;

import cn.livestream.sdk.PlayerConfig;
import cn.livestream.sdk.PlayerStats;

public interface PlayerProtocol {
    String getName();
    void initialize(PlayerConfig config);
    void play();
    void stop();
    void pause();
    void resume();
    boolean isPlaying();
    PlayerStats getStats();
    void addStatsListener(java.util.function.Consumer<PlayerStats> listener);
}