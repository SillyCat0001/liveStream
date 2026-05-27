package cn.livestream.android.player;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import cn.livestream.sdk.PlayerStats;
import org.webrtc.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class WebRTCPlayerAndroid {
    private static final String TAG = "WebRTCPlayerAndroid";

    private PeerConnectionFactory pcFactory;
    private PeerConnection pc;
    private VideoSink videoSink;
    private PlayerStats stats = new PlayerStats();
    private PlayerState state = PlayerState.IDLE;
    private List<Consumer<PlayerState>> stateListeners = new ArrayList<>();
    private List<Consumer<PlayerStats>> statsListeners = new ArrayList<>();
    private Handler mainHandler;
    private Context context;

    public String getName() {
        return "WebRTC";
    }

    public void initialize(Context context, VideoSink videoSink) {
        this.context = context;
        this.videoSink = videoSink;
        this.mainHandler = new Handler(Looper.getMainLooper());

        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(context)
                .createInitializationOptions());

        pcFactory = new PeerConnectionFactory.Builder().createPeerConnectionFactory();

        Log.d(TAG, "WebRTCPlayerAndroid initialized");
    }

    public void play() {
        if (pc == null) {
            createPeerConnection();

            MediaConstraints constraints = new MediaConstraints();
            pc.createAnswer(new SdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription sdp) {
                    pc.setLocalDescription(sdp, null);
                }

                @Override
                public void onSetSuccess() {
                    mainHandler.post(() -> updateState(PlayerState.PLAYING));
                }

                @Override
                public void onCreateFailure(String error) {
                    Log.e(TAG, "SDP create error:", error);
                    updateState(PlayerState.ERROR);
                }

                @Override
                public void onSetFailure(String error) {
                    Log.e(TAG, "SDP set error:", error);
                    updateState(PlayerState.ERROR);
                }
            }, constraints);
        }
    }

    private void createPeerConnection() {
        pc = pcFactory.createPeerConnection(
                new PeerConnection.RTCConfiguration(new ArrayList<>()),
                new PeerConnection.Observer() {
                    @Override
                    public void onSignalingChange(SignalingState state) {}

                    @Override
                    public void onIceConnectionChange(IceConnectionState iceState) {
                        if (iceState == IceConnectionState.CONNECTED) {
                            mainHandler.post(() -> updateState(PlayerState.PLAYING));
                        } else if (iceState == IceConnectionState.FAILED) {
                            mainHandler.post(() -> updateState(PlayerState.ERROR));
                        }
                    }

                    @Override
                    public void onIceGatheringChange(IceGatheringState state) {}

                    @Override
                    public void onIceCandidate(IceCandidate candidate) {}

                    @Override
                    public void onAddStream(MediaStream stream) {
                        mainHandler.post(() -> {
                            if (videoSink != null && stream.videoTracks.size() > 0) {
                                stream.videoTracks.get(0).addSink(videoSink);
                            }
                        });
                    }

                    @Override
                    public void onRemoveStream(MediaStream stream) {}

                    @Override
                    public void onDataChannel(DataChannel dataChannel) {}

                    @Override
                    public void onRenegotiationNeeded() {}
                });
    }

    public void stop() {
        if (pc != null) {
            pc.close();
            pc = null;
        }
        updateState(PlayerState.IDLE);
    }

    public void pause() {
        updateState(PlayerState.PAUSED);
    }

    public void resume() {
        updateState(PlayerState.PLAYING);
    }

    public boolean isPlaying() {
        return state == PlayerState.PLAYING;
    }

    public PlayerStats getStats() {
        return stats;
    }

    public void addStateListener(Consumer<PlayerState> listener) {
        stateListeners.add(listener);
    }

    public void addStatsListener(Consumer<PlayerStats> listener) {
        statsListeners.add(listener);
    }

    private void updateState(PlayerState newState) {
        this.state = newState;
        for (Consumer<PlayerState> listener : stateListeners) {
            listener.accept(newState);
        }
    }
}