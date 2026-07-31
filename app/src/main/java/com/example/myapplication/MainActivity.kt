package com.example.myapplication

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.myapplication.data.PlayMode
import com.example.myapplication.data.Song
import com.example.myapplication.playback.ACTION_OPEN_PLAYER
import com.example.myapplication.playback.toMediaItemOrNull
import com.example.myapplication.service.PlaybackService
import com.example.myapplication.ui.AutoMusicApp
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.viewmodel.MusicViewModel
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

//MainActivity 主要功能
//1. 连接 PlaybackService
//2. 把 UI 操作转换成 MediaController 命令
//3. 把播放器状态同步回 MusicViewModel

//整体流程：
//Compose UI
//   ↓
//MainActivity//负责 UI 交互和状态同步
//   ↓
//MediaController//MainActivity 控制播放器的工具
//   ↓
//MediaSession//连接系统和播放器
//   ↓
//PlaybackService//播放音乐
//   ↓
//ExoPlayer//执行音频播放

class MainActivity : ComponentActivity() {

    private val musicViewModel:
            MusicViewModel by viewModels()

    private var controllerFuture://异步连接 MediaController 的 Future 对象
            ListenableFuture<MediaController>? =
        null

    private var mediaController://UI 控制后台播放器的“遥控器”
            MediaController? = null


    private var progressJob://定期同步播放进度到 UI 的后台协程
            Job? =
        null

    /**
     * 每次点击系统媒体通知时自增，
     * AutoMusicApp 监听这个值并打开播放详情页。
     */
    private var openPlayerRequest by//点击系统媒体通知时自增，用于触发打开播放详情页
    mutableStateOf(0)

    private val playerListener =
        object : Player.Listener {//监听后台 ExoPlayer 的变化

            override fun onMediaItemTransition(//歌曲切换
                mediaItem: MediaItem?,
                reason: Int
            ) {
                val song =
                    findSongByMediaItem(
                        mediaItem
                    )
                        ?: return

                val addToHistory =
                    musicViewModel
                        .uiState
                        .value
                        .currentSongId !=
                            song.id

                musicViewModel
                    .onMedia3SongChanged(
                        song = song,
                        addToHistory =
                            addToHistory
                    )
            }

            override fun onIsPlayingChanged(//播放状态改变，暂停-播放
                isPlaying: Boolean
            ) {
                mediaController
                    ?.let(::syncControllerState)
            }

            override fun onPlaybackStateChanged(//播放器状态改变，正在播放，播放结束
                playbackState: Int
            ) {
                mediaController
                    ?.let(::syncControllerState)
            }

            override fun onPlayerError(//播放失败，比如歌曲url无效
                error: PlaybackException
            ) {
                Log.e(
                    "MainActivity",
                    "Media3 播放失败",
                    error
                )

                musicViewModel
                    .setPlaybackError(
                        "歌曲播放失败"
                    )
            }
        }

    override fun onCreate(//Activity 生命周期：创建 Compose UI
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        enableEdgeToEdge()
        //处理来自系统通知的点击事件
        handlePlaybackIntent(intent)

        setContent {
            MyApplicationTheme {//应用主题设置
                val uiState by
                //从 ViewModel 收集 UI 状态
                musicViewModel
                    .uiState
                    .collectAsState()


                AutoMusicApp(//这里是 UI 操作进入播放系统的入口
                    uiState = uiState,

                    onSearchTextChange =
                        musicViewModel::
                        onSearchTextChange,

                    onSongClick = {
                            song ->
                        playSongWithMedia3(
                            song
                        )
                    },

                    onPlayClick = {
                            song ->
                        playSongWithMedia3(
                            song
                        )
                    },

                    onPlayPauseClick = {
                        togglePlayPause()
                    },

                    onPreviousClick = {
                        playPrevious()
                    },

                    onNextClick = {
                        playNext()
                    },

                    onSeekForwardClick = {
                        seekBy(10_000L)
                    },

                    onSeekBackwardClick = {
                        seekBy(-10_000L)
                    },

                    onProgressChange = {
                            value ->
                        musicViewModel
                            .previewMedia3Seek(
                                value
                            )
                    },

                    onSeekFinished = {
                        finishSeek()
                    },

                    onVolumeChange = {
                            value ->
                        changeVolume(value)
                    },

                    onPlaybackSpeedChange = {
                            speed ->
                        changePlaybackSpeed(
                            speed
                        )
                    },

                    onPlayModeClick = {
                        changePlayMode()
                    },

                    onFavoriteClick =
                        musicViewModel::
                        toggleFavorite,

                    onClearPlaybackHistory =
                        musicViewModel::
                        clearPlaybackHistory,

                    onRemoveSongFromHistory =
                        musicViewModel::
                        removeSongFromHistory,

                    onLyricClick = {
                            lyricText ->
                        seekToLyric(
                            lyricText
                        )
                    },

                    onOnlineMusicClick = {
                        // 页面切换由 AutoMusicApp 内部处理
                    },

                    onRefreshOnlineSongs =
                        musicViewModel::
                        refreshOnlineSongs,

                    onAddWidgetClick = {
                        requestPinWidget()
                    },

                    openPlayerRequest =
                        openPlayerRequest
                )
            }
        }
    }

    //当用户通过通知栏重新进入应用时触发
    override fun onNewIntent(
        intent: Intent
    ) {
        super.onNewIntent(intent)

        setIntent(intent)
        handlePlaybackIntent(intent)
    }

    //Activity 进入前台后就开始连接后台播放器。（包括首次启动和从后台恢复）
    override fun onStart() {
        super.onStart()

        // 如果已有连接（onStop 保留了），只需重新添加 Listener 并同步状态
        val existing = mediaController
        if (existing != null && existing.isConnected) {
            Log.d("MainActivity", "复用已有 MediaController")
            existing.addListener(playerListener)
            syncCurrentSong(existing)
            syncControllerState(existing)
            startProgressUpdates()
            return
        }

        connectMediaController()
    }

    //Activity 进入后台时清理资源
    override fun onStop() {
        progressJob?.cancel()
        progressJob = null

        // 移除 Listener（避免 UI 刷新浪费资源），但保留 MediaController 连接
        // 这样 Widget 操作后 App 回到前台仍能看到正确状态
        mediaController?.removeListener(playerListener)

        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 真正释放 MediaController
        mediaController?.removeListener(playerListener)
        mediaController = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
    }

    //建立与 PlaybackService 的连接，获取 MediaController
    private fun connectMediaController() {//Step 2 — 创建 SessionToken（服务端的"地址"）
        //SessionToken 封装了目标 Service 的身份信息。ComponentName 包含包名和 PlaybackService 的完整类名，告诉 Media3 框架要去连接哪个 Service
        val sessionToken =
            SessionToken(
                this,
                ComponentName(//要连接的是 PlaybackService 这个服务。
                    this,
                    PlaybackService::class.java
                )
            )

        val future =
            MediaController.Builder(//Step3:异步创建 MediaController,相当于这时 MainActivity 就拿到了“遥控器”
                this,
                sessionToken
            ).buildAsync()
        //buildAsync() 返回一个 ListenableFuture<MediaController>，不会阻塞主线程。此时框架内部会：
        //  1. 解析 SessionToken 中的 ComponentName
        //  2. 通过 bindService() 绑定到 PlaybackService
        //  3. 如果 Service 还没启动，系统会先创建它


        controllerFuture = future// MediaController的引用
        //持有"正在进行的异步连接操作"的引用，以便在 onStop() 时能取消它。
        //假设用户在歌曲列表页刚点了一首歌，connectMediaController() 已经调用了 buildAsync()，
        //但Service还没返回结果——此时用户立刻按 Home 键。
        //如果不调用 releaseFuture()，连接完成后回调仍然会触发，尝试操作一个已经不存在的 Activity，导致崩溃或内存泄漏。

        future.addListener(//Step 4 — 连接成功后的回调，获取 MediaController 实例
            {
                try {
                    val controller = future.get()//获取MediaController
                    mediaController = controller


                    controller.addListener(//注册播放状态监听
                        playerListener//将播放器状态监听器连接到 MediaController
                    )

                    applyPlayMode(//同步播放模式
                        controller,
                        musicViewModel
                            .uiState
                            .value
                            .playMode//将应用设置的播放模式（单曲循环、列表循环、随机播放）应用到后台播放器
                    )

                    syncCurrentSong(//恢复当前歌曲信息
                        controller//从后台播放器恢复当前播放的歌曲到应用 UI
                        //在播放“晴天”，中途按home退出了，MainActivity进入后台，PlaybackService继续播放
                        //重新打开APP，新 MainActivity 需要知道后台现在还在播放“晴天”。
                    )

                    syncControllerState(
                        controller//同步播放器的播放状态、进度、时长到 ViewModel
                    )

                    startProgressUpdates()//启动定期更新播放进度的后台协程

                    Log.d(
                        "MainActivity",
                        "MediaController 连接 PlaybackService 成功"
                    )
                } catch (e: Exception) {
                    Log.e(
                        "MainActivity",
                        "MediaController 连接失败",
                        e
                    )
                }
            },
            ContextCompat.getMainExecutor(
                this
            )
        )
    }

    /**
     * 点击歌曲时，把全部可播放歌曲设置成 ExoPlayer 队列，
     * 并从用户点击的歌曲开始播放。
     */
    //核心播放逻辑：将歌曲列表转换为 MediaItem 队列
    private fun playSongWithMedia3(//用户点击歌曲时触发：设置播放队列并开始播放
        song: Song
    ) {
        val controller =//检查有没有连接到后台播放器
            mediaController

        if (controller == null) {//没连接到后台播放器不能播放
            Log.w(
                "MainActivity",
                "MediaController 尚未连接"
            )
            return
        }

        val playablePairs =
            musicViewModel
                .uiState
                .value
                .songs
                .mapNotNull {//遍历歌曲列表，把整个可播放的歌曲列表转换成 MediaItem 队列（ExoPlayer播放队列）
                        currentSong ->

                    currentSong
                        .toMediaItemOrNull(
                            this
                        )
                        ?.let {
                                mediaItem ->
                            currentSong to
                                    mediaItem
                        }
                }

        val startIndex =//从对应播放歌曲下标开始播放
            playablePairs
                .indexOfFirst {
                    it.first.id ==
                            song.id
                }

        if (startIndex < 0) {
            musicViewModel
                .setPlaybackError(
                    "歌曲资源不存在"
                )
            return
        }

        val mediaItems =
            playablePairs.map {
                it.second
            }

        // 立即更新应用内 UI；
        // Player.Listener 再收到同一首歌曲时不会重复加入历史。
        musicViewModel
            .onMedia3SongChanged(
                song = song,
                addToHistory = true
            )

        controller.setMediaItems(//把整个歌曲列表设置为播放队列 从startIndex开始 从歌曲 0 ms 开始播放
            mediaItems,
            startIndex,
            0L
        )

        applyPlayMode(
            controller,
            musicViewModel
                .uiState
                .value
                .playMode
        )

        controller.prepare()
        controller.play()
    }

    //切换播放/暂停状态//检查播放队列，如果为空则播放当前歌曲
    private fun togglePlayPause() {
        //MainActivity
        //↓
        //MediaController.pause()
        //↓
        //MediaSession
        //↓
        //ExoPlayer.pause()

        val controller =//检查有没有连接到后台播放器
            mediaController
                ?: return

        if (controller.mediaItemCount == 0) {
            musicViewModel
                .uiState
                .value
                .currentSong
                ?.let {
                    playSongWithMedia3(it)
                }

            return
        }

        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    //播放上一首歌曲//如果播放队列为空则不做任何操作
    private fun playPrevious() {
        val controller =
            mediaController
                ?: return

        if (controller.mediaItemCount == 0) {
            return
        }

        controller
            .seekToPreviousMediaItem()
    }

    //播放下一首歌曲//如果播放队列为空则不做任何操作
    private fun playNext() {
        val controller =
            mediaController
                ?: return

        if (controller.mediaItemCount == 0) {
            return
        }

        controller
            .seekToNextMediaItem()
    }

    //快进/快退指定毫秒数//确保目标位置在歌曲时长范围内
    private fun seekBy(
        offsetMs: Long
    ) {
        val controller =
            mediaController
                ?: return

        if (controller.mediaItemCount == 0) {
            return
        }

        val duration =
            controller.duration
                .takeIf {
                    it != C.TIME_UNSET &&
                            it > 0
                }

        val targetPosition =
            controller
                .currentPosition
                .plus(offsetMs)
                .let {
                        position ->
                    if (duration != null) {
                        position.coerceIn(
                            0L,
                            duration
                        )
                    } else {
                        position.coerceAtLeast(
                            0L
                        )
                    }
                }

        controller.seekTo(
            targetPosition
        )

        musicViewModel
            .finishMedia3Seek(
                targetPosition
                    .coerceAtMost(
                        Int.MAX_VALUE.toLong()
                    )
                    .toInt()
            )
    }

    //完成进度条拖动后的最终定位//将 ViewModel 中预览的进度应用到播放器
    private fun finishSeek() {
        val controller =
            mediaController
                ?: return

        val position =
            musicViewModel
                .uiState
                .value
                .currentPosition
                .coerceAtLeast(0)

        controller.seekTo(
            position.toLong()
        )

        musicViewModel
            .finishMedia3Seek(
                position
            )
    }

    //根据歌词文本定位到对应播放位置//找到歌词对应的时间点并跳转，如果暂停则自动开始播放
    private fun seekToLyric(
        lyricText: String
    ) {
        val controller =
            mediaController
                ?: return

        val position =
            musicViewModel
                .findLyricPosition(
                    lyricText
                )
                ?: return

        controller.seekTo(
            position.toLong()
        )

        musicViewModel
            .finishMedia3Seek(
                position
            )

        if (!controller.isPlaying) {
            controller.play()
        }
    }

    //调整播放音量//音量值限制在 0.0 到 1.0 之间
    private fun changeVolume(
        value: Float
    ) {
        val safeVolume =
            value.coerceIn(
                0f,
                1f
            )

        mediaController?.volume =
            safeVolume

        musicViewModel
            .updateMedia3Volume(
                safeVolume
            )
    }

    //调整播放速度//播放速度限制在 0.5x 到 2.0x 之间
    private fun changePlaybackSpeed(
        speed: Float
    ) {
        val safeSpeed =
            speed.coerceIn(
                0.5f,
                2f
            )

        mediaController
            ?.setPlaybackSpeed(
                safeSpeed
            )

        musicViewModel
            .updateMedia3PlaybackSpeed(
                safeSpeed
            )
    }

    //切换播放模式（单曲循环/列表循环/随机播放）//更新 ViewModel 并应用到播放器
    private fun changePlayMode() {
        musicViewModel
            .changePlayMode()

        mediaController?.let {
                controller ->
            applyPlayMode(
                controller,
                musicViewModel
                    .uiState
                    .value
                    .playMode
            )
        }
    }

    //将应用播放模式应用到 MediaController//根据 PlayMode 枚举设置播放器的循环和随机模式
    private fun applyPlayMode(
        controller: MediaController,
        playMode: PlayMode
    ) {
        when (playMode) {
            PlayMode.SINGLE_LOOP -> {
                controller.repeatMode =
                    Player.REPEAT_MODE_ONE

                controller
                    .shuffleModeEnabled =
                    false
            }

            PlayMode.LIST_LOOP -> {
                controller.repeatMode =
                    Player.REPEAT_MODE_ALL

                controller
                    .shuffleModeEnabled =
                    false
            }

            PlayMode.SHUFFLE -> {
                controller.repeatMode =
                    Player.REPEAT_MODE_ALL

                controller
                    .shuffleModeEnabled =
                    true
            }
        }
    }

    /**
     * Activity 重新进入前台时，
     * 从后台播放器恢复当前歌曲。
     */
    //同步当前播放的歌曲到应用 UI
    private fun syncCurrentSong(//确保 Activity 重新进入前台时显示正确的当前歌曲
        controller: MediaController
    ) {
        val mediaItem =
            controller.currentMediaItem
                ?: return

        val song =
            findSongByMediaItem(
                mediaItem
            )
                ?: return

        if (
            musicViewModel
                .uiState
                .value
                .currentSongId !=
            song.id
        ) {
            musicViewModel
                .onMedia3SongChanged(
                    song = song,
                    addToHistory = false
                )
        }
    }

    //根据 MediaItem 查找对应的 Song 对象//通过 mediaId 匹配歌曲 ID
    private fun findSongByMediaItem(
        mediaItem: MediaItem?
    ): Song? {
        val songId =
            mediaItem
                ?.mediaId
                ?.toIntOrNull()
                ?: return null

        return musicViewModel
            .uiState
            .value
            .songs
            .firstOrNull {
                it.id == songId
            }
    }

    //同步播放器状态到 ViewModel
    private fun syncControllerState(
        controller: MediaController
    ) {
        val safeDuration =
            controller.duration
                .takeIf {
                    it != C.TIME_UNSET &&
                            it > 0
                }
                ?: 0L

        musicViewModel
            .updateMedia3PlaybackState(
                isPlaying =
                    controller.isPlaying,

                currentPosition =
                    controller.currentPosition,

                duration =
                    safeDuration,

                playbackSpeed =
                    controller
                        .playbackParameters
                        .speed
            )
    }

    //启动定期更新播放进度的协程
    private fun startProgressUpdates() {
        progressJob?.cancel()

        progressJob =
            lifecycleScope.launch {
                while (isActive) {
                    mediaController?.let {
                            controller ->

                        // 歌曲列表可能晚于 Controller 加载完成，
                        // 因此每轮都尝试恢复一次当前歌曲，
                        // 但只有 ID 不一致时才真正更新。
                        syncCurrentSong(
                            controller
                        )

                        syncControllerState(
                            controller
                        )
                    }

                    delay(500L)
                }
            }
    }

    //处理来自系统通知的点击意图
    private fun handlePlaybackIntent(
        intent: Intent?
    ) {
        if (
            intent?.action ==
            ACTION_OPEN_PLAYER
        ) {
            openPlayerRequest += 1

            // 避免屏幕旋转或 Activity 重建时重复处理。
            intent.action = null
        }
    }

    /**
     * 绕过 HyperOS/MIUI 的 Widget 过滤器，
     * 直接将音乐 Widget 钉到桌面。
     * Android 8.0+ 支持。
     */
    private fun requestPinWidget() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            return
        }

        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(this)
        val componentName = android.content.ComponentName(
            this,
            com.example.myapplication.widget.MusicWidgetProvider::class.java
        )

        val pinned = appWidgetManager.requestPinAppWidget(
            componentName,
            null,
            null
        )

        Log.d("MainActivity", "requestPinWidget result: $pinned")
    }
}