package cn.livestream.sdk;

public interface PlayerListener {
    void onStateChanged(PlayerState state);
    void onStatsUpdated(PlayerStats stats);
    void onError(String message);
    void onProtocolSwitched(String protocol);
}