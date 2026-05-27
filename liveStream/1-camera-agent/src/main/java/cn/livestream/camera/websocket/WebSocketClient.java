package cn.livestream.camera.websocket;

import java.util.function.Consumer;

public interface WebSocketClient {
    void connect();
    void disconnect();
    void sendMessage(String message);
    void onMessage(Consumer<String> handler);
    boolean isConnected();
}