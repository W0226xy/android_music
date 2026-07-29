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

class MainActivity : ComponentActivity() {

    private val musicViewModel:
            MusicViewModel by viewModels()

    private var controllerFuture:
            ListenableFuture<MediaController>? =
        null

    private var mediaController:
            MediaController? =
        null

    private var progressJob:
            Job? =
        null

    /**
     * 每次点击系统媒体通知时自增，
     * AutoMusicApp 监听这个值并打开播放详情页。
     */
    private var openPlayerRequest by
    mutableStateOf(0)

    private val playerListener =
        object : Player.Listener {

            override fun onMediaItemTransition(
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

            override fun onIsPlayingChanged(
                isPlaying: Boolean
            ) {
                mediaController
                    ?.let(::syncControllerState)
            }

            override fun onPlaybackStateChanged(
                playbackState: Int
            ) {
                mediaController
                    ?.let(::syncControllerState)
            }

            override fun onPlayerError(
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

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        enableEdgeToEdge()

        handlePlaybackIntent(intent)

        setContent {
            MyApplicationTheme {
                val uiState by
                musicViewModel
                    .uiState
                    .collectAsState()

                AutoMusicApp(
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

                    openPlayerRequest =
                        openPlayerRequest
                )
            }
        }
    }

    override fun onNewIntent(
        intent: Intent
    ) {
        super.onNewIntent(intent)

        setIntent(intent)
        handlePlaybackIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        connectMediaController()
    }

    override fun onStop() {
        progressJob?.cancel()
        progressJob = null

        mediaController?.removeListener(
            playerListener
        )

        mediaController = null

        controllerFuture?.let {
            MediaController.releaseFuture(
                it
            )
        }

        controllerFuture = null

        super.onStop()
    }

    private fun connectMediaController() {
        val sessionToken =
            SessionToken(
                this,
                ComponentName(
                    this,
                    PlaybackService::class.java
                )
            )

        val future =
            MediaController.Builder(
                this,
                sessionToken
            ).buildAsync()

        controllerFuture =
            future

        future.addListener(
            {
                try {
                    val controller =
                        future.get()

                    mediaController =
                        controller

                    controller.addListener(
                        playerListener
                    )

                    applyPlayMode(
                        controller,
                        musicViewModel
                            .uiState
                            .value
                            .playMode
                    )

                    syncCurrentSong(
                        controller
                    )

                    syncControllerState(
                        controller
                    )

                    startProgressUpdates()

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
    private fun playSongWithMedia3(
        song: Song
    ) {
        val controller =
            mediaController

        if (controller == null) {
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
                .mapNotNull {
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

        val startIndex =
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

        controller.setMediaItems(
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

    private fun togglePlayPause() {
        val controller =
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
    private fun syncCurrentSong(
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
}