package cn.livestream.app.playerengine

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.upstream.DefaultHttpDataSource
import cn.livestream.app.ui.player.PlayerState
import cn.livestream.app.ui.player.PlayerStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class PlayerEngine(
    protected val player: ExoPlayer
) {
    abstract val name: String
    abstract fun play(url: String)
    abstract fun stop()
    abstract fun pause()
    abstract fun resume()
    open fun getStats(): PlayerStats = PlayerStats()

    protected val _state = MutableStateFlow(PlayerState.IDLE)
    val state: StateFlow<PlayerState> = _state

    protected fun updateState(newState: PlayerState) {
        _state.value = newState
    }
}

class HlsPlayerEngine(context: Context) : PlayerEngine(
    ExoPlayer.Builder(context).build()
) {
    override val name = "HLS"

    override fun play(url: String) {
        updateState(PlayerState.CONNECTING)
        val dataSourceFactory = DefaultHttpDataSource.Factory()
        val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(url))

        player.setMediaSource(mediaSource)
        player.prepare()
        player.play()
        setupListener()
        updateState(PlayerState.PLAYING)
    }

    override fun stop() {
        player.stop()
        updateState(PlayerState.IDLE)
    }

    override fun pause() {
        player.pause()
        updateState(PlayerState.PAUSED)
    }

    override fun resume() {
        player.play()
        updateState(PlayerState.PLAYING)
    }

    private fun setupListener() {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> updateState(PlayerState.BUFFERING)
                    Player.STATE_READY -> updateState(PlayerState.PLAYING)
                    Player.STATE_ENDED -> updateState(PlayerState.IDLE)
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                updateState(PlayerState.ERROR)
            }
        })
    }
}

class FlvPlayerEngine(context: Context) : PlayerEngine(
    ExoPlayer.Builder(context).build()
) {
    override val name = "HTTP-FLV"

    override fun play(url: String) {
        updateState(PlayerState.CONNECTING)
        val dataSourceFactory = DefaultHttpDataSource.Factory()
        val mediaItem = MediaItem.fromUri(url)
        val mediaSource = MediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItem)

        player.setMediaSource(mediaSource)
        player.prepare()
        player.play()
        updateState(PlayerState.PLAYING)
    }

    override fun stop() {
        player.stop()
        updateState(PlayerState.IDLE)
    }

    override fun pause() {
        player.pause()
        updateState(PlayerState.PAUSED)
    }

    override fun resume() {
        player.play()
        updateState(PlayerState.PLAYING)
    }
}