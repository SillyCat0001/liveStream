package cn.livestream.camera.websocket;

import cn.livestream.camera.config.CameraConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Component
public class StandardWebSocketClient implements WebSocketClient {
    private static final Logger log = LoggerFactory.getLogger(StandardWebSocketClient.class);

    private final CameraConfig config;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private WebSocketConnectionManager connectionManager;
    private WebSocketSession session;
    private Consumer<String> messageHandler;
    private ReconnectCallback reconnectCallback;
    private ReconnectState reconnectState = new ReconnectState();

    public StandardWebSocketClient(CameraConfig config) {
        this.config = config;
    }

    public void setReconnectCallback(ReconnectCallback callback) {
        this.reconnectCallback = callback;
    }

    @Override
    public void connect() {
        if (connected.get()) return;

        StandardWebSocketClient client = this;
        WebSocketHandler handler = new WebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession s) {
                session = s;
                connected.set(true);
                reconnectState.setReconnecting(false);
                reconnectState.resetRetry();
                if (reconnectCallback != null) {
                    reconnectCallback.onReconnectSuccess();
                }
                log.info("WebSocket connected: {}", s.getId());
            }

            @Override
            public void handleMessage(WebSocketSession s, TextMessage message) {
                if (messageHandler != null) {
                    messageHandler.accept(message.getPayload());
                }
            }

            @Override
            public void handleTransportError(WebSocketSession s, Throwable exception) {
                log.error("WebSocket transport error", exception);
                connected.set(false);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession s, CloseStatus status) {
                connected.set(false);
                log.info("WebSocket closed: {}", status);
                if (!reconnectState.isReconnecting()) {
                    reconnectState.setReconnecting(true);
                    reconnectState.incrementRetry();
                    doReconnect();
                }
            }

            @Override
            public void handlePartialMessage(WebSocketSession s, TextMessage message) {}
        };

        connectionManager = new WebSocketConnectionManager(
            new org.springframework.web.socket.client.standard.StandardWebSocketClient(),
            handler,
            config.getServer().getWebsocketUrl()
        );
        connectionManager.start();
    }

    private void doReconnect() {
        int maxAttempts = config.getServer().getMaxReconnectAttempts();
        int delay = 5000; // 固定5秒

        new Thread(() -> {
            try {
                Thread.sleep(delay);
                int attempts = reconnectState.getRetryCount();
                if (attempts < maxAttempts) {
                    log.info("Reconnect attempt {}/{}", attempts, maxAttempts);
                    connect();
                } else {
                    log.warn("Reconnect attempts exhausted ({}), stopping stream", attempts);
                    reconnectState.setReconnecting(false);
                    reconnectState.setStreamStoppedByReconnect(true);
                    if (reconnectCallback != null) {
                        reconnectCallback.onReconnectFailed(attempts);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    @Override
    public void disconnect() {
        if (connectionManager != null) {
            connectionManager.stop();
        }
        connected.set(false);
    }

    @Override
    public void sendMessage(String message) {
        if (session != null && connected.get()) {
            try {
                session.sendMessage(new TextMessage(message));
                log.debug("Sent message: {}", message);
            } catch (Exception e) {
                log.error("Failed to send message", e);
            }
        } else {
            log.warn("Cannot send message: not connected");
        }
    }

    @Override
    public void onMessage(Consumer<String> handler) {
        this.messageHandler = handler;
    }

    @Override
    public boolean isConnected() {
        return connected.get();
    }
}