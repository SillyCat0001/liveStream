package cn.livestream.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.PlayerView
import cn.livestream.app.playerengine.FlvPlayerEngine
import cn.livestream.app.playerengine.HlsPlayerEngine
import cn.livestream.app.playerengine.WebRTCPlayerEngine
import cn.livestream.app.ui.theme.Connected
import cn.livestream.app.ui.theme.Disconnected

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val context = LocalContext.current

    var playerView by remember { mutableStateOf<PlayerView?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("直播监控系统") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Video Player
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black, RoundedCornerShape(8.dp))
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).also { pv ->
                            pv.useController = true
                            playerView = pv
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Status overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            if (uiState.playerState == PlayerState.PLAYING) Connected else Disconnected,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when (uiState.playerState) {
                            PlayerState.IDLE -> "未连接"
                            PlayerState.CONNECTING -> "连接中"
                            PlayerState.BUFFERING -> "缓冲中"
                            PlayerState.PLAYING -> "播放中"
                            PlayerState.PAUSED -> "已暂停"
                            PlayerState.ERROR -> "错误"
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stream URL Input
            OutlinedTextField(
                value = uiState.streamUrl,
                onValueChange = { viewModel.updateStreamUrl(it) },
                label = { Text("流地址") },
                placeholder = { Text("输入 HLS/HTTP-FLV 地址") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Protocol Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Protocol.entries.forEach { protocol ->
                    FilterChip(
                        selected = uiState.selectedProtocol == protocol,
                        onClick = { viewModel.selectProtocol(protocol) },
                        label = {
                            Text(
                                when (protocol) {
                                    Protocol.HLS -> "HLS"
                                    Protocol.HTTP_FLV -> "HTTP-FLV"
                                    Protocol.WEBRTC -> "WebRTC"
                                    Protocol.AUTO -> "自动"
                                }
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val engine = when (uiState.selectedProtocol) {
                            Protocol.HLS -> HlsPlayerEngine(context)
                            Protocol.HTTP_FLV -> FlvPlayerEngine(context)
                            Protocol.WEBRTC -> WebRTCPlayerEngine(context)
                            Protocol.AUTO -> HlsPlayerEngine(context)
                        }
                        playerView?.player = engine.player
                        engine.play(uiState.streamUrl)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = uiState.playerState == PlayerState.IDLE ||
                              uiState.playerState == PlayerState.PAUSED
                ) {
                    Text("播放")
                }

                Button(
                    onClick = {
                        playerView?.player?.stop()
                        viewModel.stop()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = uiState.playerState != PlayerState.IDLE
                ) {
                    Text("停止")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Display
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "播放统计",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("码率: ${stats.videoBitrate} kbps")
                    Text("帧率: ${stats.fps} fps")
                    Text("延迟: ${stats.latencyMs} ms")
                    Text("缓冲: ${stats.bufferLevelMs} ms")
                }
            }
        }
    }
}
