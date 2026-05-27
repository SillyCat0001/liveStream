package cn.livestream.android.player;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import cn.livestream.sdk.PlayerStats;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class HttpFlvPlayerAndroid {
    private static final String TAG = "HttpFlvPlayerAndroid";

    private ExoPlayer player;
    private Context context;
    private View playerView;
    private PlayerStats stats = new PlayerStats();
    private PlayerState state = PlayerState.IDLE;
    private List<Consumer<PlayerState>> stateListeners = new ArrayList<>();
    private List<Consumer<PlayerStats>> statsListeners = new ArrayList<>();
    private Handler mainHandler;
    private String streamUrl;

    public String getName() {
        return "HTTP-FLV";
    }

    public void initialize(Context context, View playerView, String streamUrl) {
        this.context = context;
        this.playerView = playerView;
        this.streamUrl = streamUrl;
        this.mainHandler = new Handler(Looper.getMainLooper());

        player = new ExoPlayer.Builder(context).build();
        playerView.setPlayer(player);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                switch (playbackState) {
                    case Player.STATE_READY:
                        updateState(PlayerState.PLAYING);
                        break;
                    case Player.STATE_BUFFERING:
                        updateState(PlayerState.BUFFERING);
                        break;
                    case Player.STATE_ENDED:
                        updateState(PlayerState.IDLE);
                        break;
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Log.e(TAG, "Playback error:", error);
                updateState(PlayerState.ERROR);
            }
        });

        Log.d(TAG, "HttpFlvPlayerAndroid initialized with URL: " + streamUrl);
    }

    public void play() {
        if (player != null && streamUrl != null) {
            DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory();
            ProgressiveMediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(streamUrl));

            player.setMediaSource(mediaSource);
            player.prepare();
            player.play();
            updateState(PlayerState.CONNECTING);
            startStatsCollection();
        }
    }

    public void stop() {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
        stopStatsCollection();
        updateState(PlayerState.IDLE);
    }

    public void pause() {
        if (player != null) {
            player.pause();
            updateState(PlayerState.PAUSED);
        }
    }

    public void resume() {
        if (player != null) {
            player.play();
            updateState(PlayerState.PLAYING);
        }
    }

    public boolean isPlaying() {
        return player != null && player.isPlaying();
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

    private void startStatsCollection() {
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (player != null && isPlaying()) {
                    stats.setVideoBitrate(2000);
                    stats.setFps(30.0);
                    stats.setLatencyMs(1000);
                    notifyStatsListeners();
                    mainHandler.postDelayed(this, 1000);
                }
            }
        }, 1000);
    }

    private void stopStatsCollection() {
        mainHandler.removeCallbacksAndMessages(null);
    }

    private void updateState(PlayerState newState) {
        this.state = newState;
        for (Consumer<PlayerState> listener : stateListeners) {
            listener.accept(newState);
        }
    }

    private void notifyStatsListeners() {
        for (Consumer<PlayerStats> listener : statsListeners) {
            listener.accept(stats);
        }
    }
}