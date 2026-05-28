package cn.livestream.camera.websocket;

public interface ReconnectCallback {
    void onReconnectFailed(int attempts);
    void onReconnectSuccess();
}