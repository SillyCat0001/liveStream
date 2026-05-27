# Android 直播播放器 App 设计文档

> 日期: 2026-05-28
> 状态: 已批准

## 1. 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                         Android App                          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐   │
│  │    UI       │     │  ViewModel  │     │   Player    │   │
│  │ (Compose)   │────▶│   (State)   │────▶│   Engine    │   │
│  └─────────────┘     └─────────────┘     └──────┬──────┘   │
│                                                  │          │
│  ┌─────────────┐     ┌─────────────┐     ┌──────▼──────┐   │
│  │ Material3   │     │ Repository  │     │ ExoPlayer  │   │
│  │   Theme     │     │             │     │ libwebrtc   │   │
│  └─────────────┘     └─────────────┘     └─────────────┘   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## 2. 功能模块

| 模块 | 职责 |
|------|------|
| **PlayerScreen** | 播放界面：视频播放器、URL 输入、协议选择、播放控制 |
| **PlayerViewModel** | 播放状态管理、用户命令转发 |
| **PlayerEngine** | 底层播放器调度（根据协议选择对应 Player） |
| **HlsPlayer** | HLS 播放（ExoPlayer/Media3） |
| **FlvPlayer** | HTTP-FLV 播放（ExoPlayer/Media3） |
| **WebRTCPlayer** | WebRTC 播放（libwebrtc） |

## 3. UI 布局

```
┌─────────────────────────────┐
│  🔴 直播监控系统            │  ← 顶部栏
├─────────────────────────────┤
│                             │
│    ┌─────────────────┐      │
│    │                 │      │  ← 视频播放区域
│    │   视频播放器    │      │
│    │                 │      │
│    └─────────────────┘      │
│                             │
│  ┌─────────────────────┐    │
│  │ rtmp://...           │    │  ← URL 输入框
│  └─────────────────────┘    │
│                             │
│  [HLS▼] [▶播放] [⏹停止]    │  ← 控制按钮
│                             │
│  状态: 播放中               │  ← 状态显示
│  码率: 2000 kbps            │
│  帧率: 30 fps               │  ← 统计信息
│  延迟: 2000 ms              │
└─────────────────────────────┘
```

## 4. 技术栈

| 组件 | 选型 |
|------|------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material3 |
| 架构 | MVVM |
| 播放器 | ExoPlayer (Media3) + libwebrtc |
| 协议 | HLS, HTTP-FLV, WebRTC |
| 主题 | Material3 浅色主题 |
| minSdk | 24 |
| targetSdk | 34 |

## 5. 项目结构

```
android-player-app/
├── app/
│   └── src/main/
│       ├── java/cn/livestream/app/
│       │   ├── MainActivity.kt
│       │   ├── MainApplication.kt
│       │   ├── ui/
│       │   │   ├── theme/
│       │   │   │   ├── Color.kt
│       │   │   │   ├── Theme.kt
│       │   │   │   └── Type.kt
│       │   │   ├── player/
│       │   │   │   ├── PlayerScreen.kt
│       │   │   │   ├── PlayerViewModel.kt
│       │   │   │   └── PlayerState.kt
│       │   │   └── components/
│       │   │       └── PlayerControls.kt
│       │   ├── playerengine/
│       │   │   ├── PlayerEngine.kt
│       │   │   ├── HlsPlayer.kt
│       │   │   ├── FlvPlayer.kt
│       │   │   └── WebRTCPlayer.kt
│       │   └── repository/
│       │       └── PlayerRepository.kt
│       ├── res/
│       │   └── values/
│       │       └── strings.xml
│       └── AndroidManifest.xml
├── build.gradle
└── settings.gradle
```

## 6. PlayerEngine 设计

```kotlin
sealed class PlayerEngine {
    abstract fun play(url: String)
    abstract fun stop()
    abstract fun pause()
    abstract fun resume()
    abstract fun getStats(): PlayerStats
    abstract fun getName(): String
}

class HlsPlayerEngine : PlayerEngine { ... }
class FlvPlayerEngine : PlayerEngine { ... }
class WebRTCPlayerEngine : PlayerEngine { ... }
```

## 7. 状态定义

```kotlin
enum class PlayerState {
    IDLE,       // 未连接
    CONNECTING, // 连接中
    BUFFERING,  // 缓冲中
    PLAYING,    // 播放中
    PAUSED,     // 已暂停
    ERROR       // 错误
}

data class PlayerStats(
    val videoBitrate: Int = 0,    // kbps
    val fps: Double = 0.0,
    val latencyMs: Long = 0,
    val bufferLevelMs: Long = 0
)
```

## 8. 依赖

```groovy
dependencies {
    // Compose BOM
    implementation platform('androidx.compose:compose-bom:2024.02.00')
    implementation 'androidx.compose.ui:ui'
    implementation 'androidx.compose.material3:material3'
    
    // ViewModel
    implementation 'androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0'
    
    // ExoPlayer (Media3)
    implementation 'androidx.media3:media3-exoplayer:1.2.1'
    implementation 'androidx.media3:media3-exoplayer-hls:1.2.1'
    
    // WebRTC
    implementation 'io.getstream:stream-webrtc-android:1.1.1'
}
```