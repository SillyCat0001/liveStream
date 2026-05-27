package cn.livestream.sdk;

public interface LivePlayer {
    void play(PlayerConfig config);
    void stop();
    void pause();
    void resume();
    PlayerState getState();
    PlayerStats getStats();
    void addListener(PlayerListener listener);
    void removeListener(PlayerListener listener);
}