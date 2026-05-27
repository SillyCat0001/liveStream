package cn.livestream.app.ui.player

enum class PlayerState {
    IDLE,
    CONNECTING,
    BUFFERING,
    PLAYING,
    PAUSED,
    ERROR
}

data class PlayerStats(
    val videoBitrate: Int = 0,
    val fps: Double = 0.0,
    val latencyMs: Long = 0,
    val bufferLevelMs: Long = 0
)

enum class Protocol {
    HLS,
    HTTP_FLV,
    WEBRTC,
    AUTO
}