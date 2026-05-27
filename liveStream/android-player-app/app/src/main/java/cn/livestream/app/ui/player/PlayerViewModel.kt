package cn.livestream.app.ui.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.livestream.app.playerengine.FlvPlayerEngine
import cn.livestream.app.playerengine.HlsPlayerEngine
import cn.livestream.app.playerengine.PlayerEngine
import cn.livestream.app.playerengine.WebRTCPlayerEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlayerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState

    private val _stats = MutableStateFlow(PlayerStats())
    val stats: StateFlow<PlayerStats> = _stats

    private var currentEngine: PlayerEngine? = null

    fun selectProtocol(protocol: Protocol) {
        _uiState.value = _uiState.value.copy(selectedProtocol = protocol)
    }

    fun updateStreamUrl(url: String) {
        _uiState.value = _uiState.value.copy(streamUrl = url)
    }

    fun play(context: Context) {
        val state = _uiState.value
        if (state.streamUrl.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "请输入流地址")
            return
        }

        currentEngine?.stop()

        currentEngine = when (state.selectedProtocol) {
            Protocol.HLS -> HlsPlayerEngine(context)
            Protocol.HTTP_FLV -> FlvPlayerEngine(context)
            Protocol.WEBRTC -> WebRTCPlayerEngine(context)
            Protocol.AUTO -> HlsPlayerEngine(context)
        }

        viewModelScope.launch {
            currentEngine?.state?.collect { playerState ->
                _uiState.value = _uiState.value.copy(playerState = playerState)
            }
        }

        currentEngine?.play(state.streamUrl)
        startStatsCollection()
    }

    fun stop() {
        currentEngine?.stop()
        currentEngine = null
        _uiState.value = _uiState.value.copy(playerState = PlayerState.IDLE)
        _stats.value = PlayerStats()
    }

    fun pause() {
        currentEngine?.pause()
    }

    fun resume() {
        currentEngine?.resume()
    }

    private fun startStatsCollection() {
        viewModelScope.launch {
            while (true) {
                currentEngine?.let { engine ->
                    _stats.value = engine.getStats()
                }
                delay(1000)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

data class PlayerUiState(
    val streamUrl: String = "",
    val selectedProtocol: Protocol = Protocol.AUTO,
    val playerState: PlayerState = PlayerState.IDLE,
    val errorMessage: String? = null
)