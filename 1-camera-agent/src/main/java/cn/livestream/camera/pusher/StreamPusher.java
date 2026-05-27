package cn.livestream.camera.pusher;

import cn.livestream.camera.config.CameraConfig;

public interface StreamPusher {
    void start(CameraConfig config);
    void stop();
    boolean isRunning();
    void updateBitrate(int bitrate);
}
