# Android Music Player

基于 Jetpack Compose 的智能音乐播放器

## 1. 项目简介

### 1.1 项目名称

**Android Music Player** —— 基于 Jetpack Compose 的智能音乐播放器

### 1.2 项目介绍

本项目是一款基于 Android 平台开发的音乐播放器应用，采用 Kotlin 与 Java 语言实现，使用 Jetpack Compose 构建现代化 UI 界面。

项目支持本地音乐播放、在线音乐获取、歌词解析展示、收藏管理、播放历史记录以及后台持续播放等功能。

客户端采用 MVVM 架构设计，通过 ViewModel 管理播放器状态，实现 UI 与业务逻辑解耦。

同时搭建 Spring Boot 音乐服务器，实现在线歌曲管理、网络音乐获取以及客户端数据接口服务。

---

## 2. 项目技术架构

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────┐
│                    Android Client                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────────┐  │
│  │   UI     │  │ ViewModel │  │    Repository        │  │
│  │ Compose  │◄─┤  State   ├──┤  Local / Online       │  │
│  └──────────┘  └──────────┘  └──────────┬───────────┘  │
│                                         │               │
│  ┌──────────────────────────────────────┼───────────┐  │
│  │         PlaybackService (Media3)     │           │  │
│  │  ┌──────────┐  ┌─────────────────┐   │           │  │
│  │  │ ExoPlayer │  │  MediaSession   │   │           │  │
│  │  └──────────┘  └─────────────────┘   │           │  │
│  └──────────────────────────────────────┼───────────┘  │
│                                         │               │
│  ┌──────────┐  ┌────────────────────────┼───────────┐  │
│  │  Widget  │  │    Retrofit (网络层)    │           │  │
│  │ (Glance) │  └────────────┬───────────┘           │  │
│  └──────────┘               │                        │
└──────────────────────────────┼────────────────────────┘
                               │ HTTP
┌──────────────────────────────┼────────────────────────┐
│                    Spring Boot Server                   │
│  ┌──────────────┐  ┌─────────┐  ┌──────────────────┐  │
│  │ SongController│  │ SongService │  │  SongMapper   │  │
│  └──────┬───────┘  └────┬────┘  └────────┬─────────┘  │
│         │               │                │             │
│  ┌──────┴───────────────┴────────────────┴─────────┐  │
│  │                   MySQL                          │  │
│  └─────────────────────────────────────────────────┘  │
│                                                       │
│  ┌──────────────────────────────────────────────┐    │
│  │    Jamendo API (在线音乐资源)                  │    │
│  └──────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────┘
```

---

## 3. 功能模块设计

### 3.1 本地音乐播放模块

**功能描述**：实现手机本地音乐资源加载、播放控制以及播放状态管理。用户可以查看本地歌曲列表，并进行播放、暂停、切换歌曲、拖动进度条、跳转至指定歌词位置播放、调整音量大小等操作。

#### 3.1.1 歌曲列表展示

功能：应用启动后，通过预定义的本地音乐资源获取歌曲信息，并构建歌曲列表。用户可以在音乐列表页面查看歌曲名称、歌手等基本信息，点击对应歌曲后进入播放流程。

#### 3.1.2 播放控制

功能：实现音乐播放过程中的基础控制操作，包括播放、暂停、上一首以及下一首歌曲切换。

播放器状态由 `MusicViewModel` 统一管理，通过状态变化驱动 UI 更新。

#### 3.1.3 播放进度控制

功能：实现音乐播放过程中的进度管理，包括实时进度显示、拖动进度条调整播放位置以及快进/快退操作。

### 3.2 歌词解析与歌词显示

**功能描述**：歌词模块实现歌曲歌词文件的解析、展示以及播放过程中的歌词状态管理。

#### 3.2.1 歌词解析

针对本地歌曲，通过读取应用内部 LRC 文件资源，实现歌词内容解析。在线歌曲，通过服务器返回的歌词 URL 获取歌词文件，并展示对应歌曲歌词。

#### 3.2.2 歌词同步显示

播放歌曲过程中，根据播放器当前时间匹配对应歌词，实现歌词实时同步。

#### 3.2.3 歌词窗口管理

采用歌词窗口，仅展示当前歌词附近的部分内容，在播放过程中窗口自动滚动至对应歌词位置。

### 3.3 播放列表管理

负责歌曲队列管理，支持播放模式（单曲循环、列表循环、随机播放）控制。

### 3.4 歌曲收藏管理

用户可以添加收藏、取消收藏，收藏 UI 根据是否收藏状态动态刷新。

### 3.5 歌曲搜索功能

搜索模块支持用户根据歌曲名称或歌手信息快速查找目标歌曲，提高歌曲管理效率。

#### 3.5.1 实时搜索功能

用户输入关键词时，系统实时监听输入内容，并动态过滤歌曲列表。

#### 3.5.2 模糊匹配功能

搜索过程中忽略大小写，根据关键词匹配歌曲名称或歌手。

### 3.6 播放历史模块

用于记录用户最近播放过的歌曲，方便用户快速查找和再次播放。

#### 3.6.1 播放历史保存

当用户播放歌曲时，将歌曲加入播放历史。记录信息包括：歌曲名称、歌手、播放时间、歌曲来源（本地/在线）。

#### 3.6.2 播放历史展示

播放历史页面展示用户最近播放歌曲。支持查看历史歌曲、点击历史歌曲继续播放、按播放顺序排列。

#### 3.6.3 播放历史删除

支持清空播放历史、删除单条播放记录。

### 3.7 在线音乐模块

在线音乐模块实现客户端通过网络获取服务器提供的歌曲资源，并支持在线歌曲列表展示、歌曲信息加载、在线音频播放、歌词获取以及歌曲封面展示等功能。

#### 3.7.1 在线歌曲列表获取

用户进入在线音乐页面时，客户端通过 Retrofit 定义服务器接口，向服务器请求在线歌曲列表。服务器返回包含歌曲名称、歌手、音频地址、歌词地址以及封面地址等信息的 JSON 数据，客户端通过 Gson 自动解析为 Kotlin 对象，并更新 UI 状态，展示在线歌曲列表。

#### 3.7.2 Spring Boot 歌曲接口服务

服务器基于 Spring Boot 框架提供 REST API 接口，为 Android 客户端提供在线歌曲数据服务。客户端通过 HTTP 请求访问服务器接口，服务器负责处理请求、查询歌曲信息，并将结果转换为 JSON 格式返回。

#### 3.7.3 网络音乐资源获取与保存

支持服务器从网络音乐平台 Jamendo 获取歌曲资源。用户在请求在线歌曲时优先查询 MySQL 数据库有没有保存的在线歌曲缓存，如果有则返回数据库信息，如果没有则调用 Jamendo API 获取歌曲信息（歌曲名称、歌手、歌词、音频 URL、封面 URL）并将获取的歌曲信息保存至 MySQL 数据库，形成歌曲缓存。

#### 3.7.4 在线音乐播放功能

用户点击在线歌曲后，客户端根据服务器返回的音频 URL，通过 Media3 ExoPlayer 加载网络音频资源，实现在线播放。

### 3.8 后台播放功能

后台播放模块实现应用退出当前播放页面甚至切换到后台后，音乐仍能够持续播放的功能。

#### 3.8.1 Media3 播放器架构

将原本 Activity 内部管理的播放器逻辑迁移至独立的 `PlaybackService`，使播放器生命周期不再依赖页面生命周期。

#### 3.8.2 PlaybackService 后台播放服务

`PlaybackService` 作为后台音乐播放核心组件，负责播放器生命周期管理和音乐播放任务执行。主要职责：创建 ExoPlayer 实例、管理 MediaSession 响应播放控制指令、维护后台播放状态。

#### 3.8.3 MediaSession 媒体会话管理

`MediaSession` 用于连接应用播放器和 Android 系统媒体框架，实现统一的媒体控制接口。接收客户端播放控制、同步播放器状态、支持系统媒体按钮。

#### 3.8.4 MediaController 播放器控制功能

`MainActivity` 通过 `MediaController` 与 `PlaybackService` 建立连接，实现前台界面对后台播放器的控制。

### 3.9 桌面小组件（App Widget）

**功能描述**：桌面小组件模块实现在手机桌面上直接查看当前播放歌曲信息并进行基本播放控制，无需打开 App 即可完成播放/暂停、切歌等操作。

#### 3.9.1 技术选型

小组件 UI 采用 **Jetpack Glance** 框架实现，基于 Compose 声明式 UI 构建，替代传统的 RemoteViews + XML 布局方式。

| 对比维度 | RemoteViews (旧) | Glance Compose (新) |
|---------|-----------------|-------------------|
| UI 定义 | XML 布局文件 | Kotlin @Composable 函数 |
| 布局能力 | 受限的 LinearLayout / FrameLayout | Row / Column / Box 等完整布局 |
| 状态管理 | 手动 `setTextViewText` / `setImageViewBitmap` | `currentState()` + `PreferencesGlanceStateDefinition` |
| 点击事件 | `PendingIntent.getActivity()` | `actionStartActivity(intent)` |
| 代码复用 | 无法复用 App 内 Compose 代码 | 与 App 内 Compose 风格一致 |

**技术栈**：
- `androidx.glance:glance-appwidget:1.1.1` — Glance AppWidget 核心
- `androidx.glance:glance-material3:1.1.1` — Material3 主题支持
- `androidx.datastore:datastore-preferences` — 小组件状态持久化

#### 3.9.2 小组件 UI 布局

采用深色圆角卡片布局，类似系统媒体播放器 Widget：

```
┌────────────────────────────────────┐
│        深色圆角卡片 (16dp)          │
│                                    │
│  [封面56dp]   歌曲名称 (白/加粗)    │
│  圆角8dp      歌手 (灰色)          │
│                                    │
│  00:08  ═══════●═══════  06:59    │
│        进度条 + 实时时间            │
│                                    │
│  ⏮    ◁◁    ▶/⏸    ▷▷    ⏭     │
│ 上一首 后退  播放暂停  前进  下一首 │
│              (绿色高亮)             │
└────────────────────────────────────┘
```

**布局元素**：
- **专辑封面**（左侧 56dp 正方圆角）：有封面时显示当前歌曲封面，无封面时显示半透明占位
- **歌曲信息**（右侧）：歌名单行加粗白色 + 歌手灰色小字
- **播放进度**（中部）：当前时间 + `LinearProgressIndicator` + 总时长，跟随播放器实时更新
- **控制按钮**（底部 5 个）：
  - `⏮` 上一首 / `◁◁` 后退 10 秒 / `▶⏸` 播放暂停（绿色高亮）/ `▷▷` 前进 10 秒 / `⏭` 下一首
  - 所有按钮通过 `WidgetCommandActivity` → `MediaController` 执行控制指令

#### 3.9.3 架构设计

```
PlaybackService
      │
      │ sendBroadcast(ACTION_PLAYBACK_STATE_CHANGED)
      │ sendBroadcast(ACTION_PLAYBACK_PROGRESS)
      ▼
MusicWidgetProvider (GlanceAppWidgetReceiver)
      │
      ├── onReceive()    → 处理广播，解析歌曲信息
      ├── loadArtwork()  → 加载封面 Bitmap（支持本地 + 网络）
      ├── updateAppWidgetState() → 写入 DataStore Preferences
      │
      └── MusicGlanceWidget (GlanceAppWidget)
              │
              └── provideGlance() → Compose UI
                      │
                      ├── currentState() → 读取 WidgetState
                      ├── AlbumArt (ImageProvider)
                      ├── Text (歌名 / 歌手)
                      └── ControlButton × 3
                              │
                              └── actionStartActivity(Intent)
                                      │
                                      ▼
                              WidgetCommandActivity
                                      │
                                      └── MediaController
                                              │
                                              ├── play() / pause()
                                              ├── seekToNextMediaItem()
                                              ├── seekToPreviousMediaItem()
                                              └── seekTo()
```

#### 3.9.4 核心类

| 类名 | 职责 |
|------|------|
| `MusicWidgetProvider` | `GlanceAppWidgetReceiver` 子类，接收播放状态广播，加载封面图片，管理 Widget 生命周期 |
| `MusicGlanceWidget` | `GlanceAppWidget` 子类，定义 Compose UI 布局 |
| `WidgetStateKeys` | `Preferences.Key` 定义集合，用于 Glance DataStore 状态持久化 |
| `WidgetCommandActivity` | 透明 Activity，接收按钮点击的 Intent，通过 `MediaController` 执行播放控制指令 |
| `PlaybackService` | 后台播放服务，通过 `widgetStateListener` 监听播放器状态变化，发送广播通知 Widget 更新 |

#### 3.9.5 数据流

**播放状态 → Widget 更新流程**：

1. `PlaybackService` 中 `Player.Listener` 检测到歌曲切换或播放状态变化
2. 防抖 150ms 后通过 `sendBroadcast()` 发送 `ACTION_PLAYBACK_STATE_CHANGED` 广播
3. `MusicWidgetProvider.onReceive()` 拦截广播，提取歌曲信息（名称、歌手、封面 URI、是否播放中）
4. 主线程立即更新文本状态并调用 `MusicGlanceWidget().updateAll(context)`
5. IO 线程异步加载封面 Bitmap（支持 content:// / http:// 两种来源，LRU 缓存）
6. 封面加载完成后保存到 `cacheDir` 文件，再次更新 Widget 状态

**用户点击按钮 → 播放控制流程**：

1. Glance Compose 中按钮的 `clickable` 触发 `actionStartActivity(intent)`
2. `Intent` 携带对应 Action（`ACTION_WIDGET_PLAY` / `PAUSE` / `NEXT` / `PREVIOUS` 等）
3. `WidgetCommandActivity.onCreate()` 读取 `intent.action`
4. 通过 `SessionToken` → `MediaController.Builder` 异步获取 `MediaController`
5. 执行对应控制指令，延迟 300ms 后释放 `MediaController` 并 `finish()`

#### 3.9.6 封面加载策略

- **图片来源**：支持本地文件（`content://` / `android.resource://`）和网络文件（`http://` / `https://`）
- **缓存层级**：内存 LRU 缓存（容量 5）+ 磁盘文件缓存（`cacheDir/aw_*.png`）
- **内存优化**：通过 `BitmapFactory.Options.inSampleSize` 按目标尺寸（256px）降采样
- **无封面处理**：显示半透明圆角方块占位，不显示错误图片

#### 3.9.7 异步更新策略

为避免 Widget 更新阻塞主线程，采用分阶段更新：

1. **阶段一**（主线程，<5ms）：仅更新文本状态（歌名、歌手、播放状态），封面先置空
2. **阶段二**（IO 线程，异步）：加载并缓存封面图片到本地文件
3. **阶段三**（主线程，封面就绪后）：更新封面路径到 Widget 状态

此策略保证 Widget 文字信息始终即时响应，封面在后台加载完成后平滑出现。

#### 3.9.8 兼容性说明

- **最低支持**：Android 7.0 (API 24)，`minSdk = 24`
- **Glance 版本**：1.1.1，要求 `compileSdk >= 33`
- **HyperOS / MIUI 注意**：部分小米系统对非 MIUI Widget 有过滤逻辑，建议：
  - `initialLayout` 使用极简占位布局，避免 `?android:attr/` 主题引用
  - 自定义广播 Action 与 `APPWIDGET_UPDATE` 分别声明，不混入同一 `intent-filter`
  - 必要时通过 `requestPinAppWidget()` API 绕过系统 Widget 选择器
