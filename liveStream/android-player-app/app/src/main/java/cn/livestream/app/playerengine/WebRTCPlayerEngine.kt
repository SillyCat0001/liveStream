package cn.livestream.app.playerengine

import android.content.Context
import cn.livestream.app.ui.player.PlayerState
import cn.livestream.app.ui.player.PlayerStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.webrtc.*

class WebRTCPlayerEngine(context: Context) : PlayerEngine(
    ExoPlayer.Builder(context).build()
) {
    override val name = "WebRTC"

    private var pcFactory: PeerConnectionFactory? = null
    private var pc: PeerConnection? = null
    private val appContext = context

    init {
        val options = PeerConnectionFactory.InitializationOptions.builder(appContext)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
        pcFactory = PeerConnectionFactory.builder()
            .createPeerConnectionFactory()
    }

    override fun play(url: String) {
        updateState(PlayerState.CONNECTING)

        val config = PeerConnection.RTCConfiguration(ArrayList()).apply {
            withContinualGathering(true)
        }

        pc = pcFactory?.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onSignalingChange(state: SignalingState?) {}
            override fun onIceConnectionChange(state: IceConnectionState?) {
                when (state) {
                    IceConnectionState.CONNECTED -> updateState(PlayerState.PLAYING)
                    IceConnectionState.FAILED -> updateState(PlayerState.ERROR)
                    IceConnectionState.DISCONNECTED -> updateState(PlayerState.IDLE)
                    else -> {}
                }
            }
            override fun onIceGatheringChange(state: IceGatheringState?) {}
            override fun onIceCandidate(candidate: IceCandidate?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
        })

        // Demo: use local camera as source
        val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", null)
        val videoCapturer = Camera2Enumerator(appContext).createCapturer(0, surfaceTextureHelper)

        val videoSource = pcFactory?.createVideoSource(videoCapturer)
        videoCapturer?.startCapture(1280, 720, 30)

        val videoTrack = pcFactory?.createVideoTrack("video", videoSource)
        pc?.addTrack(videoTrack, ArrayList())

        updateState(PlayerState.PLAYING)
    }

    override fun stop() {
        pc?.close()
        pc = null
        updateState(PlayerState.IDLE)
    }

    override fun pause() {
        updateState(PlayerState.PAUSED)
    }

    override fun resume() {
        updateState(PlayerState.PLAYING)
    }

    override fun getStats(): PlayerStats {
        return PlayerStats(
            videoBitrate = 2000,
            fps = 30.0,
            latencyMs = 1000,
            bufferLevelMs = 3000
        )
    }
}