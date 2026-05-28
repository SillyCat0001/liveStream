package cn.livestream.camera.websocket;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReconnectState {
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    private final AtomicInteger retryCount = new AtomicInteger(0);
    private volatile boolean streamStoppedByReconnect = false;

    public boolean isReconnecting() { return reconnecting.get(); }
    public void setReconnecting(boolean v) { reconnecting.set(v); }
    public int getRetryCount() { return retryCount.get(); }
    public void incrementRetry() { retryCount.incrementAndGet(); }
    public void resetRetry() { retryCount.set(0); }
    public boolean isStreamStoppedByReconnect() { return streamStoppedByReconnect; }
    public void setStreamStoppedByReconnect(boolean v) { this.streamStoppedByReconnect = v; }
}