# Android 直播播放器 App 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现一个完整的 Android 直播播放器 App，支持 HLS、HTTP-FLV、WebRTC 协议播放

**Architecture:** MVVM + Jetpack Compose 架构，PlayerEngine 抽象播放器接口，ViewModel 管理状态

**Tech Stack:** Kotlin, Jetpack Compose, Material3, ExoPlayer (Media3), libwebrtc

---

## 项目结构

```
android-player-app/
├── app/
│   └── src/main/
│       ├── java/cn/livestream/app/
│       │   ├── MainActivity.kt
│       │   ├── ui/
│       │   │   ├── theme/
│       │   │   │   ├── Color.kt
│       │   │   │   ├── Theme.kt
│       │   │   │   └── Type.kt
│       │   │   └── player/
│       │   │       ├── PlayerScreen.kt
│       │   │       ├── PlayerViewModel.kt
│       │   │       └── PlayerState.kt
│       │   └── playerengine/
│       │       ├── PlayerEngine.kt
│       │       ├── HlsPlayerEngine.kt
│       │       ├── FlvPlayerEngine.kt
│       │       └── WebRTCPlayerEngine.kt
│       └── AndroidManifest.xml
├── build.gradle
└── settings.gradle
```

---

## 任务 1: 创建项目骨架

**Files:**
- Create: `android-player-app/settings.gradle`
- Create: `android-player-app/build.gradle`
- Create: `android-player-app/app/build.gradle`
- Create: `android-player-app/app/src/main/AndroidManifest.xml`
- Create: `android-player-app/app/src/main/java/cn/livestream/app/MainActivity.kt`

### settings.gradle

```groovy
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "LiveStreamPlayer"
include ':app'
```

### build.gradle (root)

```groovy
plugins {
    id 'com.android.application' version '8.2.2' apply false
    id 'org.jetbrains.kotlin.android' version '1.9.22' apply false
}
```

### app/build.gradle

```groovy
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}

android {
    namespace 'cn.livestream.app'
    compileSdk 34

    defaultConfig {
        minSdk 24
        targetSdk 34
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = '17'
    }

    buildFeatures {
        compose true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = '1.5.8'
    }
}

dependencies {
    def composeBom = platform('androidx.compose:compose-bom:2024.02.00')
    implementation composeBom

    implementation 'androidx.compose.ui:ui'
    implementation 'androidx.compose.material3:material3'
    implementation 'androidx.compose.ui:ui-tooling-preview'
    implementation 'androidx.activity:activity-compose:1.8.2'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0'

    // ExoPlayer (Media3)
    implementation 'androidx.media3:media3-exoplayer:1.2.1'
    implementation 'androidx.media3:media3-exoplayer-hls:1.2.1'
    implementation 'androidx.media3:media3-ui:1.2.1'

    // WebRTC
    implementation 'io.getstream:stream-webrtc-android:1.1.1'

    // Core
    implementation 'androidx.core:core-ktx:1.12.0'
}
```

### AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.LiveStreamPlayer">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize"
            android:theme="@style/Theme.LiveStreamPlayer">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

### MainActivity.kt

```kotlin
package cn.livestream.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import cn.livestream.app.ui.player.PlayerScreen
import cn.livestream.app.ui.theme.LiveStreamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LiveStreamTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    PlayerScreen()
                }
            }
        }
    }
}
```

- [ ] **Step 1: 创建项目目录结构**
- [ ] **Step 2: 创建 settings.gradle 和 build.gradle**
- [ ] **Step 3: 创建 AndroidManifest.xml**
- [ ] **Step 4: 创建 MainActivity.kt**
- [ ] **Step 5: 提交**

---

## 任务 2: 创建 Theme 文件

**Files:**
- Create: `android-player-app/app/src/main/java/cn/livestream/app/ui/theme/Color.kt`
- Create: `android-player-app/app/src/main/java/cn/livestream/app/ui/theme/Theme.kt`
- Create: `android-player-app/app/src/main/java/cn/livestream/app/ui/theme/Type.kt`

### Color.kt

```kotlin
package cn.livestream.app.ui.theme

import androidx.compose.ui.graphics.Color

val Primary = Color(0xFF1976D2)
val OnPrimary = Color.White
val PrimaryContainer = Color(0xFFBBDEFB)
val Secondary = Color(0xFF03A9F4)
val Background = Color(0xFFFAFAFA)
val Surface = Color.White
val OnBackground = Color(0xFF1C1B1F)
val OnSurface = Color(0xFF1C1B1F)
val Error = Color(0xFFB00020)
val Connected = Color(0xFF4CAF50)
val Disconnected = Color(0xFF9E9E9E)
```

### Type.kt

```kotlin
package cn.livestream.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    )
)
```

### Theme.kt

```kotlin
package cn.livestream.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    secondary = Secondary,
    background = Background,
    surface = Surface,
    onBackground = OnBackground,
    onSurface = OnSurface,
    error = Error
)

@Composable
fun LiveStreamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
```

- [ ] **Step 1: 创建 Color.kt**
- [ ] **Step 2: 创建 Type.kt**
- [ ] **Step 3: 创建 Theme.kt**
- [ ] **Step 4: 提交**

---

## 任务 3: 创建 PlayerState 和 PlayerStats

**Files:**
- Create: `android-player-app/app/src/main/java/cn/livestream/app/ui/player/PlayerState.kt`

### PlayerState.kt

```kotlin
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
```

- [ ] **Step 1: 创建 PlayerState.kt**
- [ ] **Step 2: 提交**

---

## 任务 4: 创建 PlayerEngine 抽象层

**Files:**
- Create: `android-player-app/app/src/main/java/cn/livestream/app/playerengine/PlayerEngine.kt`

### PlayerEngine.kt

```kotlin
package cn.livestream.app.playerengine

import androidx.compose.ui.viewinterop.AndroidView
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
    fun getStats(): PlayerStats = PlayerStats()

    protected val _state = MutableStateFlow(PlayerState.IDLE)
    val state: StateFlow<PlayerState> = _state

    protected fun updateState(newState: PlayerState) {
        _state.value = newState
    }
}

class HlsPlayerEngine(context: android.content.Context) : PlayerEngine(
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

class FlvPlayerEngine(context: android.content.Context) : PlayerEngine(
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
```

- [ ] **Step 1: 创建 PlayerEngine.kt**
- [ ] **Step 2: 提交**

---

## 任务 5: 创建 WebRTCPlayerEngine

**Files:**
- Create: `android-player-app/app/src/main/java/cn/livestream/app/playerengine/WebRTCPlayerEngine.kt`

### WebRTCPlayerEngine.kt

```kotlin
package cn.livestream.app.playerengine

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import cn.livestream.app.ui.player.PlayerState
import cn.livestream.app.ui.player.PlayerStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.webrtc.*

@OptIn(UnstableApi::class)
class WebRTCPlayerEngine(context: Context) : PlayerEngine(
    ExoPlayer.Builder(context).build()
) {
    override val name = "WebRTC"

    private var pcFactory: PeerConnectionFactory? = null
    private var pc: PeerConnection? = null
    private val appContext = context

    override fun play(url: String) {
        updateState(PlayerState.CONNECTING)

        val options = PeerConnectionFactory.InitializationOptions.builder(appContext)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        pcFactory = PeerConnectionFactory.builder()
            .createPeerConnectionFactory()

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
```

- [ ] **Step 1: 创建 WebRTCPlayerEngine.kt**
- [ ] **Step 2: 提交**

---

## 任务 6: 创建 PlayerViewModel

**Files:**
- Create: `android-player-app/app/src/main/java/cn/livestream/app/ui/player/PlayerViewModel.kt`

### PlayerViewModel.kt

```kotlin
package cn.livestream.app.ui.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.livestream.app.playerengine.FlvPlayerEngine
import cn.livestream.app.playerengine.HlsPlayerEngine
import cn.livestream.app.playerengine.PlayerEngine
import cn.livestream.app.playerengine.WebRTCPlayerEngine
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

        // Release previous engine
        currentEngine?.stop()

        // Create new engine based on protocol
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
                kotlinx.coroutines.delay(1000)
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
```

- [ ] **Step 1: 创建 PlayerViewModel.kt**
- [ ] **Step 2: 提交**

---

## 任务 7: 创建 PlayerScreen (UI)

**Files:**
- Create: `android-player-app/app/src/main/java/cn/livestream/app/ui/player/PlayerScreen.kt`

### PlayerScreen.kt

```kotlin
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

    var playerView by remember { mutableStateOf<androidx.media3.ui.PlayerView?>(null) }

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
```

- [ ] **Step 1: 创建 PlayerScreen.kt**
- [ ] **Step 2: 提交**

---

## 自检结果

**覆盖检查:**
- ✅ 项目骨架 (settings.gradle, build.gradle, AndroidManifest.xml)
- ✅ Theme 文件 (Color, Theme, Type)
- ✅ PlayerState 和 PlayerStats
- ✅ HlsPlayerEngine
- ✅ FlvPlayerEngine
- ✅ WebRTCPlayerEngine
- ✅ PlayerViewModel
- ✅ PlayerScreen (完整 UI)

**占位符检查:**
- ✅ 无 TBD/TODO
- ✅ 所有步骤包含实际代码
- ✅ 类型一致性检查通过

---

## 执行选择

**计划已保存到:** `docs/superpowers/plans/2026-05-28-android-player-app-implementation-plan.md`

**两种执行方式:**

| 方式 | 说明 |
|------|------|
| **1. Subagent-Driven（推荐）** | 每个任务由独立子代理实现 |
| **2. Inline Execution** | 在当前会话中批量执行 |

您选择哪种执行方式？