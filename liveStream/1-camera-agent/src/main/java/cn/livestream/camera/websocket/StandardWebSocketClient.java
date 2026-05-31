package cn.livestream.camera.websocket;

import cn.livestream.camera.config.ServerConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.WebSocketConnectionManager;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

@Component
public class StandardWebSocketClient implements WebSocketClient {
    private static final Logger log = LoggerFactory.getLogger(StandardWebSocketClient.class);

    private final ServerConfig serverConfig;
    private final ReconnectCallback reconnectCallback;
    private final Consumer<String> messageHandler;
    private final WebSocketConnectionFactory connectionFactory;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final ReentrantLock connectLock = new ReentrantLock();
    private final Condition connectedCond = connectLock.newCondition();
    private WebSocketConnectionManager connectionManager;
    private WebSocketSession session;
    private ReconnectState reconnectState = new ReconnectState();

    public StandardWebSocketClient(ServerConfig serverConfig,
                                   @Lazy ReconnectCallback reconnectCallback,
                                   @Lazy StreamMessageHandler messageHandler,
                                   WebSocketConnectionFactory connectionFactory) {
        this.serverConfig = serverConfig;
        this.reconnectCallback = reconnectCallback;
        this.messageHandler = messageHandler::handleMessage;
        this.connectionFactory = connectionFactory;
    }

    @PostConstruct
    public void init() {
        connect();
        log.info("WebSocket client initialized");
    }

    private static final int CONNECT_TIMEOUT_MS = 5000;

    private final WebSocketHandler handler = new WebSocketHandler() {

        @Override
        public void afterConnectionEstablished(WebSocketSession s) {
            session = s;
            connected.set(true);
            connectLock.lock();
            try {
                connectedCond.signal();
            } finally {
                connectLock.unlock();
            }
            reconnectState.setReconnecting(false);
            reconnectState.resetRetry();
            if (reconnectCallback != null) {
                reconnectCallback.onReconnectSuccess();
            }
            log.info("WebSocket connected: {}", s.getId());
        }

        @Override
        public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
            if (messageHandler != null) {
                messageHandler.accept((String) message.getPayload());
            }
        }

        @Override
        public void handleTransportError(WebSocketSession s, Throwable exception) {
            log.error("WebSocket transport error: {}", exception.getMessage());
            connected.set(false);
            if (!reconnectState.isReconnecting()) {
                reconnectState.setReconnecting(true);
                doReconnectInNewThread();
            }
        }

        @Override
        public void afterConnectionClosed(WebSocketSession s, CloseStatus status) {
            connected.set(false);
            log.info("WebSocket closed: {}", status);
            if (!reconnectState.isReconnecting()) {
                reconnectState.setReconnecting(true);
                doReconnectInNewThread();
            }
        }

        @Override
        public boolean supportsPartialMessages() {
            return false;
        }
    };

    @Override
    public void connect() {
        if (connected.get()) return;

        connectLock.lock();
        try {
            if (connectionManager != null) {
                connectionManager.stop();
            }
            connectionManager = connectionFactory.create(handler);
            connectionManager.start();
            boolean signaled = connectedCond.await(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!signaled) {
                log.warn("WebSocket connection timeout ({}ms), reconnecting", CONNECT_TIMEOUT_MS);
                connectionManager.stop();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            connectLock.unlock();
        }
        if (!connected.get()) {
            doReconnectInNewThread();
        }
    }

    private void doReconnectInNewThread() {
        reconnectState.incrementRetry();
        int attempts = reconnectState.getRetryCount();
        int maxAttempts = serverConfig.getMaxReconnectAttempts();
        if (attempts >= maxAttempts) {
            log.warn("Reconnect attempts exhausted ({}), stopping stream", attempts);
            reconnectState.setStreamStoppedByReconnect(true);
            if (reconnectCallback != null) {
                reconnectCallback.onReconnectFailed(attempts);
            }
            return;
        }
        int delay = serverConfig.getReconnectInterval();
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                log.info("Reconnect attempt {}/{}", attempts, maxAttempts);
                connect();
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
        // kept for interface compatibility, actual handler injected via constructor
    }

    @Override
    public boolean isConnected() {
        return connected.get();
    }
}