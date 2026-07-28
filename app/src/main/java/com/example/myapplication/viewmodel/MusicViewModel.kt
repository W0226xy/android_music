package com.example.myapplication.viewmodel
import kotlinx.coroutines.flow.update
import android.app.Application
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.LyricLine
import com.example.myapplication.data.MusicUiState
import com.example.myapplication.data.PlayMode
import com.example.myapplication.data.Song
import com.example.myapplication.data.SongSource
import com.example.myapplication.data.nextMode
import com.example.myapplication.repository.MusicRepository
import com.example.myapplication.utils.LyricParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.collections.emptyList

class MusicViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val repository = MusicRepository()

    private var mediaPlayer: MediaPlayer? = null
    private var lyricLines: List<LyricLine> = emptyList()
    private var isUserSeeking = false

    private var songs: List<Song> = emptyList()

    private val playbackHistory = mutableListOf<Song>()
    private val historyLimit = 100

    private val _uiState = MutableStateFlow(
        MusicUiState()
    )

    val uiState: StateFlow<MusicUiState> =
        _uiState.asStateFlow()

    init {
        loadSongs()
        startProgressLoop()
    }

    /**
     * 加载本地歌曲 + 服务器歌曲
     */
    private fun loadSongs() {
        viewModelScope.launch {//协程处理网络异步，因为网络请求要发送请求、等待服务器、最后返回数据。不能因此阻塞主线程

            Log.d(
                "MusicViewModel",
                "loadSongs start"
            )


            val localSongs = repository.getLocalSongs()

            val onlineSongs = repository.getOnlineSongs()

            Log.d(
                "MusicViewModel",
                "onlineSongs=$onlineSongs"
            )

            songs = localSongs + onlineSongs

            _uiState.value = _uiState.value.copy(
                songs = songs,
                currentSongId = songs.firstOrNull()?.id ?: 0
            )

            songs.firstOrNull()?.let {
                loadLyrics(it)
            }
        }
    }

    fun onSearchTextChange(text: String) {
        _uiState.value = _uiState.value.copy(
            searchText = text
        )
    }

    /**
     * 播放歌曲
     * LOCAL: 播放res/raw
     * ONLINE: 播放网络url
     */
    fun playSong(song: Song) {

        // 释放上一首歌曲的 MediaPlayer
        mediaPlayer?.release()
        mediaPlayer = null

        // 清空上一首歌曲的歌词状态
        lyricLines = emptyList()

        _uiState.value = _uiState.value.copy(
            currentSongId = song.id,
            isPlaying = false,
            currentPosition = 0,
            duration = 0,
            currentLyric = "等待歌词...",
            nextLyric = "",
            lyricWindow = emptyList(),
            activeLyricIndex = -1,
            fullLyricLines = emptyList(),
            currentLyricIndex = -1,
            isPlainLyrics = false,
            playbackSpeed = 1f
        )

        val player = MediaPlayer()
        mediaPlayer = player

        try {
            if (song.source == SongSource.LOCAL) {

                val audioResId = song.audioResId

                // 防止本地歌曲没有音频资源
                if (audioResId == null || audioResId == 0) {
                    Log.e(
                        "MusicViewModel",
                        "本地歌曲资源不存在：${song.name}"
                    )

                    _uiState.value = _uiState.value.copy(
                        currentLyric = "音频资源不存在",
                        isPlaying = false
                    )

                    player.release()
                    mediaPlayer = null
                    return
                }

                val uri = Uri.parse(
                    "android.resource://${context.packageName}/$audioResId"
                )

                player.setDataSource(
                    context,
                    uri
                )

                // 本地资源可以同步准备
                player.prepare()

                // 加载本地带时间戳歌词
                loadLyrics(song)

                player.setVolume(
                    _uiState.value.volume,
                    _uiState.value.volume
                )

                player.setOnCompletionListener {
                    handleSongCompletion(song)
                }

                player.setOnErrorListener { _, what, extra ->
                    Log.e(
                        "MusicViewModel",
                        "本地歌曲播放错误：what=$what，extra=$extra"
                    )

                    _uiState.value = _uiState.value.copy(
                        isPlaying = false
                    )

                    true
                }

                player.start()

                _uiState.value = _uiState.value.copy(
                    currentSongId = song.id,
                    isPlaying = true,
                    currentPosition = 0,
                    duration = player.duration,
                    playbackSpeed = 1f
                )

                addToPlaybackHistory(song)

            } else {

                val url = song.url

                if (url.isNullOrBlank()) {
                    Log.e(
                        "MusicViewModel",
                        "在线歌曲地址为空：${song.name}"
                    )

                    _uiState.value = _uiState.value.copy(
                        currentLyric = "歌曲地址不存在",
                        isPlaying = false
                    )

                    player.release()
                    mediaPlayer = null
                    return
                }

                Log.d(
                    "MusicViewModel",
                    "开始加载在线歌曲：name=${song.name}, id=${song.id}, url=$url"
                )

                player.setDataSource(url)

                player.setVolume(
                    _uiState.value.volume,
                    _uiState.value.volume
                )

                player.setOnPreparedListener { preparedPlayer ->

                    Log.d(
                        "MusicViewModel",
                        "网络歌曲准备完成：${song.name}"
                    )

                    /*
                     * 在线歌词通过服务器接口加载：
                     * GET /songs/{id}/lyrics
                     */
                    loadOnlineLyrics(song)

                    preparedPlayer.start()

                    Log.d(
                        "MusicViewModel",
                        "在线歌曲开始播放，duration=${preparedPlayer.duration}"
                    )

                    _uiState.value = _uiState.value.copy(
                        currentSongId = song.id,
                        isPlaying = true,
                        currentPosition = 0,
                        duration = preparedPlayer.duration,
                        playbackSpeed = 1f
                    )

                    addToPlaybackHistory(song)
                }

                player.setOnCompletionListener {
                    handleSongCompletion(song)
                }

                player.setOnErrorListener { _, what, extra ->

                    Log.e(
                        "MusicViewModel",
                        "在线歌曲播放错误：what=$what，extra=$extra，song=${song.name}"
                    )

                    _uiState.value = _uiState.value.copy(
                        isPlaying = false
                    )

                    true
                }

                // 在线歌曲必须异步准备
                player.prepareAsync()
            }

        } catch (e: Exception) {

            Log.e(
                "MusicViewModel",
                "播放歌曲失败：${song.name}",
                e
            )

            player.release()

            if (mediaPlayer === player) {
                mediaPlayer = null
            }

            _uiState.value = _uiState.value.copy(
                isPlaying = false,
                currentLyric = "歌曲播放失败"
            )
        }
    }


    fun playOrPause() {

        val player = mediaPlayer
        val state = _uiState.value
        val currentSong = state.currentSong ?: return

        if (state.isPlaying) {

            player?.pause()

            _uiState.value = state.copy(
                isPlaying = false
            )

        } else {

            if (player == null) {

                playSong(currentSong)

            } else {

                player.start()

                _uiState.value = state.copy(
                    isPlaying = true
                )
            }
        }
    }


    fun playNextSong() {

        val state = _uiState.value

        val nextSong =
            if (state.playMode == PlayMode.SHUFFLE && songs.size > 1) {

                songs.filter {
                    it.id != state.currentSongId
                }.random()

            } else {

                val index = songs.indexOfFirst {
                    it.id == state.currentSongId
                }

                val nextIndex =
                    if (index == -1)
                        0
                    else
                        (index + 1) % songs.size

                songs[nextIndex]
            }

        playSong(nextSong)
    }


    fun playPreviousSong() {

        val state = _uiState.value

        val previousSong =
            if (state.playMode == PlayMode.SHUFFLE && songs.size > 1) {

                songs.filter {
                    it.id != state.currentSongId
                }.random()

            } else {

                val index = songs.indexOfFirst {
                    it.id == state.currentSongId
                }

                val previousIndex =
                    if (index <= 0)
                        songs.lastIndex
                    else
                        index - 1

                songs[previousIndex]
            }

        playSong(previousSong)
    }
    fun changePlayMode() {
        _uiState.value = _uiState.value.copy(
            playMode = _uiState.value.playMode.nextMode()
        )
    }


    fun changeVolume(value: Float) {

        val volume = value.coerceIn(0f, 1f)

        mediaPlayer?.setVolume(
            volume,
            volume
        )

        _uiState.value = _uiState.value.copy(
            volume = volume
        )
    }


    fun changePlaybackSpeed(speed: Float) {

        val newSpeed = speed.coerceIn(
            0.5f,
            2.0f
        )

        mediaPlayer?.let { player ->

            val params = player.playbackParams

            params.speed = newSpeed

            player.playbackParams = params
        }

        _uiState.value = _uiState.value.copy(
            playbackSpeed = newSpeed
        )
    }


    fun onProgressChange(value: Float) {

        isUserSeeking = true

        val position = value.toInt()

        _uiState.value = _uiState.value.copy(
            currentPosition = position
        )

        updateLyric(position)
    }


    fun onSeekFinished() {

        val position =
            _uiState.value.currentPosition

        mediaPlayer?.seekTo(
            position.coerceAtLeast(0)
        )

        isUserSeeking = false
    }


    fun onLyricClick(lyricText: String) {

        val lyricLine =
            lyricLines.find {
                it.text == lyricText
            }

        if (lyricLine != null) {

            isUserSeeking = true

            mediaPlayer?.seekTo(
                lyricLine.timeMs
            )

            _uiState.value = _uiState.value.copy(
                currentPosition = lyricLine.timeMs
            )

            updateLyric(
                lyricLine.timeMs
            )

            isUserSeeking = false

            if (!_uiState.value.isPlaying) {
                playOrPause()
            }
        }
    }


    fun seekForward10s() {

        val player = mediaPlayer ?: return

        val newPosition =
            (player.currentPosition + 10000)
                .coerceAtMost(player.duration)

        player.seekTo(newPosition)

        _uiState.value = _uiState.value.copy(
            currentPosition = newPosition
        )

        updateLyric(newPosition)
    }


    fun seekBackward10s() {

        val player = mediaPlayer ?: return

        val newPosition =
            (player.currentPosition - 10000)
                .coerceAtLeast(0)

        player.seekTo(newPosition)

        _uiState.value = _uiState.value.copy(
            currentPosition = newPosition
        )

        updateLyric(newPosition)
    }


// ================= 收藏 =================


    fun toggleFavorite(song: Song) {

        val favorites =
            _uiState.value.favoriteSongIds

        val newFavorites =
            if (favorites.contains(song.id)) {
                favorites - song.id
            } else {
                favorites + song.id
            }

        _uiState.value = _uiState.value.copy(
            favoriteSongIds = newFavorites
        )
    }


// ================= 公开的收藏方法 =================


    fun onFavoriteClick(song: Song) {
        toggleFavorite(song)
    }


// ================= 播放历史 =================


    private fun addToPlaybackHistory(song: Song) {

        playbackHistory.removeAll {
            it.id == song.id
        }

        playbackHistory.add(
            0,
            song
        )

        if (playbackHistory.size > historyLimit) {
            playbackHistory.removeAt(
                playbackHistory.lastIndex
            )
        }

        _uiState.value = _uiState.value.copy(
            playbackHistory = playbackHistory.toList()
        )
    }


    fun clearPlaybackHistory() {

        playbackHistory.clear()

        _uiState.value = _uiState.value.copy(
            playbackHistory = emptyList()
        )
    }


    fun removeSongFromHistory(song: Song) {

        playbackHistory.removeAll {
            it.id == song.id
        }

        _uiState.value = _uiState.value.copy(
            playbackHistory = playbackHistory.toList()
        )
    }

    private fun handleSongCompletion(song: Song) {

        when (_uiState.value.playMode) {

            PlayMode.SINGLE_LOOP -> {

                mediaPlayer?.seekTo(0)
                mediaPlayer?.start()

                _uiState.value =
                    _uiState.value.copy(
                        currentPosition = 0,
                        isPlaying = true
                    )
            }


            PlayMode.LIST_LOOP -> {

                playNextSong()

            }


            PlayMode.SHUFFLE -> {

                playNextSong()

            }
        }
    }



    private fun loadLyrics(song: Song) {

        viewModelScope.launch {

            lyricLines =
                when (song.source) {

                    SongSource.LOCAL -> {

                        _uiState.value = _uiState.value.copy(
                            isPlainLyrics = false
                        )

                        if (song.lyricResId != null) {

                            LyricParser.parseLrc(
                                context,
                                song.lyricResId
                            )

                        } else {

                            emptyList()

                        }
                    }


                    SongSource.ONLINE -> {

                        LyricParser.parseNetworkLrc(
                            song.lyricUrl
                        )

                    }
                }


            updateLyric(0)
        }
    }

    private fun updateLyric(position: Int) {

        if (_uiState.value.isPlainLyrics) {
            return
        }

        if (lyricLines.isEmpty()) {

            _uiState.value =
                _uiState.value.copy(
                    currentLyric = "暂无歌词",
                    nextLyric = "",
                    lyricWindow = listOf("暂无歌词"),
                    activeLyricIndex = 0,
                    fullLyricLines = emptyList()
                )

            return
        }


        val currentIndex =
            lyricLines.indexOfLast {
                position >= it.timeMs
            }


        val safeIndex =
            if (currentIndex >= 0)
                currentIndex
            else
                0


        val startIndex =
            (safeIndex - 2)
                .coerceAtLeast(0)


        val endIndex =
            (startIndex + 4)
                .coerceAtMost(
                    lyricLines.lastIndex
                )


        val realStartIndex =
            (endIndex - 4)
                .coerceAtLeast(0)


        val lyricWindow =
            lyricLines
                .subList(
                    realStartIndex,
                    endIndex + 1
                )
                .map {
                    it.text
                }


        _uiState.value =
            _uiState.value.copy(
                currentLyric =
                    lyricLines[safeIndex].text,

                nextLyric =
                    lyricLines.getOrNull(
                        safeIndex + 1
                    )?.text ?: "",

                lyricWindow = lyricWindow,

                activeLyricIndex =
                    safeIndex - realStartIndex,

                fullLyricLines =
                    lyricLines.map {
                        it.text
                    },

                currentLyricIndex = safeIndex
            )
    }



    private fun startProgressLoop() {

        viewModelScope.launch {

            while (isActive) {

                val player = mediaPlayer

                if (player != null && !isUserSeeking) {

                    val position =
                        player.currentPosition

                    _uiState.value =
                        _uiState.value.copy(
                            currentPosition = position,
                            duration = player.duration
                        )

                    updateLyric(position)
                }

                delay(500)
            }
        }
    }



    override fun onCleared() {

        super.onCleared()

        mediaPlayer?.release()

        mediaPlayer = null
    }

    private fun loadOnlineLyrics(song: Song) {
        viewModelScope.launch {

            // 开始加载前，清空上一首歌曲的歌词
            _uiState.value = _uiState.value.copy(
                currentLyric = "等待歌词...",
                nextLyric = "",
                lyricWindow = emptyList(),
                activeLyricIndex = -1,
                fullLyricLines = emptyList(),
                currentLyricIndex = -1
            )

            try {

                Log.d(
                    "MusicViewModel",
                    "开始请求在线歌词：song=${song.name}, id=${song.id}"
                )
                val lyricsText =
                    repository.getOnlineLyrics(
                        (song.serverId ?: song.id).toLong()
                    )


                Log.d(
                    "MusicViewModel",
                    "歌词请求完成：song=${song.name}, id=${song.id}, length=${lyricsText.length}, content=${lyricsText.take(100)}"
                )

                // 当前播放歌曲已经切换，则放弃这次请求结果
                if (_uiState.value.currentSongId != song.id) {
                    return@launch
                }

                if (lyricsText.isBlank()) {
                    Log.d(
                        "MusicViewModel",
                        "在线歌曲暂无歌词：${song.name}"
                    )

                    _uiState.value = _uiState.value.copy(
                        currentLyric = "等待歌词...",
                        nextLyric = "",
                        lyricWindow = emptyList(),
                        activeLyricIndex = -1,
                        fullLyricLines = emptyList(),
                        currentLyricIndex = -1
                    )

                    return@launch
                }

                if (LyricParser.hasTimestamp(lyricsText)) {
                    // 有时间戳：继续使用原来的歌词同步逻辑
                    val timedLyrics =
                        LyricParser.parseTimedLyrics(lyricsText)

                    lyricLines = timedLyrics

                    updateLyric(0)

                    _uiState.value = _uiState.value.copy(
                        currentLyric = "等待歌词...",
                        nextLyric = "",
                        lyricWindow = emptyList(),
                        activeLyricIndex = -1,
                        fullLyricLines = emptyList(),
                        currentLyricIndex = -1
                    )

                    Log.d(
                        "MusicViewModel",
                        "时间轴歌词加载成功，歌曲=${song.name}，行数=${timedLyrics.size}"
                    )
                } else {
                    // 无时间戳：完整显示，用户手动滚动
                    val plainLyrics =
                        LyricParser.parsePlainLyrics(lyricsText)

                    lyricLines = emptyList()

                    _uiState.value = _uiState.value.copy(
                        currentLyric = "",
                        nextLyric = "",
                        lyricWindow = emptyList(),
                        activeLyricIndex = -1,
                        fullLyricLines = plainLyrics,
                        currentLyricIndex = -1,
                        isPlainLyrics = true
                    )

                    Log.d(
                        "MusicViewModel",
                        "普通歌词加载成功，歌曲=${song.name}，行数=${plainLyrics.size}"
                    )
                }
            } catch (e: Exception) {
                Log.e(
                    "MusicViewModel",
                    "在线歌词加载失败，歌曲=${song.name}",
                    e
                )

                // 只有当前仍在播放这首歌时才更新状态
                if (_uiState.value.currentSongId == song.id) {
                    _uiState.value = _uiState.value.copy(
                        currentLyric = "等待歌词...",
                        nextLyric = "",
                        lyricWindow = emptyList(),
                        activeLyricIndex = -1,
                        fullLyricLines = emptyList(),
                        currentLyricIndex = -1
                    )
                }
            }
        }
    }

    fun refreshOnlineSongs() {

        if (_uiState.value.isRefreshingOnlineSongs) {
            return
        }

        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    isRefreshingOnlineSongs = true
                )
            }

            try {

                Log.d(
                    "MusicViewModel",
                    "开始刷新在线歌曲"
                )

                val newOnlineSongs =
                    repository.refreshOnlineSongs()

                Log.d(
                    "MusicViewModel",
                    "刷新完成，在线歌曲数量=${newOnlineSongs.size}"
                )

                if (newOnlineSongs.isNotEmpty()) {

                    _uiState.update { currentState ->

                        // 保留所有非在线歌曲，也就是本地歌曲
                        val localSongs =
                            currentState.songs.filter {
                                it.source != SongSource.ONLINE
                            }

                        currentState.copy(
                            songs = localSongs + newOnlineSongs,
                            isRefreshingOnlineSongs = false
                        )
                    }

                } else {

                    _uiState.update {
                        it.copy(
                            isRefreshingOnlineSongs = false
                        )
                    }
                }

            } catch (e: Exception) {

                Log.e(
                    "MusicViewModel",
                    "刷新在线歌曲失败",
                    e
                )

                _uiState.update {
                    it.copy(
                        isRefreshingOnlineSongs = false
                    )
                }
            }
        }
    }
}