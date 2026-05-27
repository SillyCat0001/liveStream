# 直播平台 Demo 设计文档

> 日期: 2026-05-27
> 方案: A (轻量级原型)

## 1. 系统架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           家用摄像头远程监控平台                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────┐         ┌─────────────────┐         ┌─────────────────┐  │
│  │   摄像头端    │         │    AWS 云服务     │         │    客户端       │  │
│  │  (Java+FFmpeg)│         │                  │         │                │  │
│  │              │  RTMP   │  ┌───────────┐  │         │  ┌─────────┐  │  │
│  │  · 采集视频   │────────▶│  │  AWS IVS  │  │◀────────│  │   Web   │  │  │
│  │  · 编码 H.264 │         │  │ (主路线)   │  │ HLS/WebRTC│ │ H5 Player│  │  │
│  │  · 推送到RTMP │         │  └───────────┘  │         │  └─────────┘  │  │
│  │              │         │        │        │         │                │  │
│  │              │         │        ▼        │         │  ┌─────────┐  │  │
│  │              │         │  ┌───────────┐  │◀────────│  │Android  │  │  │
│  │              │         │  │  EC2+SRS  │  │ HLS/RTMP │  │   SDK   │  │  │
│  └──────────────┘         │  │ (备份路线) │  │         │  └─────────┘  │  │
│                           │  └───────────┘  │         │                │  │
│                           │                  │         │  ┌─────────┐  │  │
│                           └──────────────────│─────────│  │   iOS   │  │  │
│                                              │         │  └─────────┘  │  │
│                           AWS IVS            │         └─────────────────┘  │
│                           ┌──────────────────│─────────┘
│                           │  · 自动转码 H.264 │
│                           │  · 多码率输出     │
│                           │  · HLS/WebRTC     │
│                           │  · 全球CDN分发    │
│                           └──────────────────┘
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**核心流程**：
1. 摄像头（设备端）采集视频 → Java 调用 FFmpeg 编码 → RTMP 推流
2. AWS IVS 接收 RTMP → 转码 → 输出 HLS/WebRTC
3. 客户端根据网络状况自动选择最佳协议拉流

## 2. 核心组件设计

### 2.1 组件总览

| 组件 | 职责 | 关键类/接口 |
|------|------|-------------|
| **CameraAgent** | 摄像头设备端运行器 | `CameraApplication.java` |
| **FFmpegWrapper** | FFmpeg 封装，进程管理 | `FFmpegWrapper.java` |
| **StreamPusher** | RTMP 推流核心 | `RTMPPusher.java` |
| **StreamGateway** | 云端协议网关 | `StreamGateway.java` |
| **IVSBridge** | AWS IVS 集成 | `IVSBridge.java` |
| **SRSBridge** | SRS 备份集成 | `SRSBridge.java` |
| **H5Player** | Web 端播放器 | `player.html` |
| **ProtocolSelector** | 协议自动选择 | `ProtocolSelector.java` |

### 2.2 数据流

```
摄像头采集
    │
    ▼
┌────────┐    YUV     ┌────────┐    H.264    ┌────────┐    RTMP
│ 采集器  │ ────────▶ │ 编码器  │ ──────────▶ │ 封装器  │ ──────▶ AWS IVS
└────────┘           └────────┘              └────────┘
     │                   │                        │
     │                   │                        ▼
     │                   │                  ┌────────────┐
     │                   │                  │  AWS IVS   │
     │                   │                  │ HLS/WebRTC│
     │                   │                  └─────┬─────┘
     │                   │                        │       │
     ▼                   ▼                        ▼       ▼
  本地预览            状态上报                  HLS    WebRTC
```

## 3. 客户端 SDK 设计

### 3.1 多端 SDK 架构

```
                          ┌─────────────────┐
                          │  PlayerCore     │
                          │  (公共播放器核心) │
                          │  · 缓冲管理      │
                          │  · 协议抽象      │
                          │  · 错误重试      │
                          └────────┬────────┘
                                   │
           ┌───────────────────────┼───────────────────────┐
           ▼                       ▼                       ▼
    ┌─────────────┐        ┌─────────────┐        ┌─────────────┐
    │ HLSPlayer   │        │WebRTCPlayer │        │RTMPPlayer   │
    │ (HLS协议)   │        │(WebRTC协议) │        │(RTMP协议)   │
    └─────────────┘        └─────────────┘        └─────────────┘
           │                       │                       │
           └───────────────────────┼───────────────────────┘
                                   ▼
                          ┌─────────────────┐
                          │ProtocolSelector │
                          │ (自动协议选择)   │
                          └────────┬────────┘
                                   │
    ┌──────────────────────────────┼──────────────────────────────┐
    ▼                              ▼                              ▼
 ┌────────┐                   ┌────────┐                    ┌────────┐
 │  Web   │                   │Android │                    │  iOS   │
 │  SDK   │                   │  SDK   │                    │  SDK   │
 └────────┘                   └────────┘                    └────────┘
```

### 3.2 协议自动选择策略

| 条件 | 选择协议 | 原因 |
|------|----------|------|
| 网络良好 + 低延迟需求 | WebRTC | <1s 延迟 |
| 网络一般 + 跨平台优先 | HLS | 穿透性好 |
| 网络差 + 需要快速切换 | HTTP-FLV | 抗抖动 |
| 首次加载 + 不确定网络 | HLS | 兼容性最好 |

## 4. 关键接口定义

### 4.1 设备端接口

```java
// 摄像头配置
public class CameraConfig {
    private String rtmpUrl;        // rtmp://host/app/stream
    private String streamKey;       // 流的唯一标识
    private int width = 1920;
    private int height = 1080;
    private int fps = 30;
    private int videoBitrate = 2000;  // kbps
    private int audioBitrate = 128;   // kbps
    private String codec = "h264";    // h264/h265
}

// 推流服务接口
public interface StreamPusher {
    void start(CameraConfig config);
    void stop();
    boolean isRunning();
    void updateBitrate(int bitrate);
}
```

### 4.2 云端网关接口

```java
// 频道信息
public class ChannelInfo {
    private String channelId;      // IVS 频道ID
    private String streamKey;      // 推流密钥
    private String playbackUrlHls;
    private String playbackUrlWebRTC;
    private ChannelState state;
}

public enum ChannelState {
    ACTIVE, STOPPED, ERROR
}

// 网关服务接口
public interface StreamGateway {
    ChannelInfo createChannel();
    ChannelInfo getChannel(String channelId);
    void stopChannel(String channelId);
    String getPlaybackUrl(String channelId, String protocol);
}
```

### 4.3 客户端接口

```java
// 播放器配置
public class PlayerConfig {
    private String streamUrl;       // 拉流地址
    private boolean autoSwitchProtocol = true;
    private int bufferMs = 3000;    // 缓冲时长 ms
    private int maxBufferMs = 10000;
}

// 播放器核心
public interface LivePlayer {
    void play(PlayerConfig config);
    void stop();
    void pause();
    void resume();
    PlayerState getState();
    PlayerStats getStats();  // 码率、延迟、帧率
    void addListener(PlayerListener listener);
}

// 统计信息
public class PlayerStats {
    private int videoBitrate;
    private int audioBitrate;
    private double fps;
    private long latencyMs;
    private int bufferLevelMs;
}
```

## 5. 错误处理与容灾

### 5.1 故障处理策略

| 故障场景 | 处理策略 |
|----------|----------|
| 推流中断 | 本地重连（3次，间隔2s/4s/8s），重启FFmpeg进程 |
| IVS 主线路故障 | 自动切换到 SRS 备份线路，DNS 切换 Route53 |
| 拉流失败 | 协议降级（HLS→FLV→RTMP） |
| 客户端网络抖动 | 增大缓冲，降低码率适应网络 |

### 5.2 健康检查机制

- 摄像头端每 30s 检查一次
- 异常阈值：丢帧率>5%、重连次数>10次/分钟、CPU>80%

## 6. 项目结构与依赖

### 6.1 项目目录结构

```
liveStream-demo/
├── camera-agent/                  # 摄像头设备端（Java）
│   ├── src/main/java/cn/livestream/camera/
│   │   ├── CameraApplication.java
│   │   ├── config/CameraConfig.java
│   │   ├── ffmpeg/FFmpegWrapper.java
│   │   ├── pusher/RTMPPusher.java
│   │   └── health/HealthChecker.java
│   └── pom.xml
│
├── stream-gateway/                # 云端网关（Java）
│   ├── src/main/java/cn/livestream/gateway/
│   │   ├── GatewayApplication.java
│   │   ├── controller/ChannelController.java
│   │   ├── service/StreamGateway.java, IVSBridge.java, SRSBridge.java
│   │   └── model/ChannelInfo.java
│   └── pom.xml
│
├── client-sdk/                    # 多端 SDK
│   ├── player-core/
│   ├── protocol-hls/
│   ├── protocol-webrtc/
│   └── protocol-rtmp/
│
├── web-player/                   # Web 端（H5）
│   ├── index.html
│   └── js/
│
├── android-sdk/                  # Android SDK
├── ios-sdk/                      # iOS SDK
└── docker/                      # Docker 配置
```

### 6.2 技术依赖

| 组件 | 依赖 | 版本 |
|------|------|------|
| CameraAgent | Spring Boot | 3.2.x |
| | JavaCV (FFmpeg 封装) | 1.5.x |
| StreamGateway | Spring Boot | 3.2.x |
| | AWS SDK (IVS) | 2.20.x |
| Web Player | video.js | 8.x |