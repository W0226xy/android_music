package com.example.myapplication.viewmodel

import android.app.Application
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.myapplication.data.LyricContent
/**
 * ViewModel 不再持有 MediaPlayer。
 *
 * 播放器由 PlaybackService 中的 ExoPlayer 持有，
 * MainActivity 通过 MediaController 控制播放器，
 * ViewModel 只维护 Compose 需要显示的状态、歌词、收藏和播放历史。
 */
class MusicViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val context =
        application.applicationContext

    private val repository =
        MusicRepository(context)

    private var songs: List<Song> =
        emptyList()

    private var lyricLines: List<LyricLine> =
        emptyList()

    private var isUserSeeking =
        false

    private var isLyricsLoading =
        false

    private val playbackHistory =
        mutableListOf<Song>()

    private val historyLimit =
        100

    private val _uiState =
        MutableStateFlow(MusicUiState())

    val uiState: StateFlow<MusicUiState> =
        _uiState.asStateFlow()

    init {
        loadSongs()
    }

    /**
     * 加载本地歌曲和在线歌曲。
     */
    private fun loadSongs() {

        viewModelScope.launch {

            Log.d(
                "MusicViewModel",
                "loadSongs start"
            )

            songs =
                repository.getAllSongs()

            Log.d(
                "MusicViewModel",
                "songs=$songs"
            )

            val oldCurrentId =
                _uiState.value.currentSongId

            val newCurrentId =
                if (
                    songs.any {
                        it.id == oldCurrentId
                    }
                ) {
                    oldCurrentId
                } else {
                    songs.firstOrNull()?.id ?: 0
                }

            _uiState.update {
                it.copy(
                    songs = songs,
                    currentSongId = newCurrentId
                )
            }

            songs.firstOrNull {
                it.id == newCurrentId
            }?.let { song ->

                if (
                    _uiState.value.currentPosition == 0 &&
                    !_uiState.value.isPlaying
                ) {
                    loadSongLyrics(song)
                }
            }
        }
    }

    fun onSearchTextChange(
        text: String
    ) {
        _uiState.update {
            it.copy(searchText = text)
        }
    }

    /**
     * Media3 当前歌曲发生变化时调用。
     *
     * @param addToHistory 用户主动播放或真正切歌时为 true；
     * Activity 重新连接后台播放器时为 false。
     */
    fun onMedia3SongChanged(
        song: Song,
        addToHistory: Boolean
    ) {
        val isSameSong =
            _uiState.value.currentSongId == song.id

        // Activity 重新连接时，如果歌曲没有变化，
        // 不重复加载歌词，也不重复加入历史。
        if (isSameSong && !addToHistory) {
            return
        }

        lyricLines =
            emptyList()

        isUserSeeking =
            false

        isLyricsLoading =
            true

        _uiState.update {
            it.copy(
                currentSongId = song.id,
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
        }

        if (addToHistory) {
            addToPlaybackHistory(song)
        }

        loadSongLyrics(song)
    }

    /**
     * MainActivity 定时读取 MediaController 后，
     * 将播放器状态同步到 Compose UI。
     */
    fun updateMedia3PlaybackState(
        isPlaying: Boolean,
        currentPosition: Long,
        duration: Long,
        playbackSpeed: Float
    ) {
        val safePosition =
            currentPosition
                .coerceAtLeast(0L)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()

        val safeDuration =
            duration
                .coerceAtLeast(0L)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()

        val safeSpeed =
            playbackSpeed.coerceIn(
                0.5f,
                2f
            )

        if (isUserSeeking) {
            _uiState.update {
                it.copy(
                    isPlaying = isPlaying,
                    duration = safeDuration,
                    playbackSpeed = safeSpeed
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isPlaying = isPlaying,
                currentPosition = safePosition,
                duration = safeDuration,
                playbackSpeed = safeSpeed
            )
        }

        updateLyric(safePosition)
    }

    /**
     * 用户拖动进度条时，仅预览 UI，不马上 seek。
     */
    fun previewMedia3Seek(
        value: Float
    ) {
        isUserSeeking =
            true

        val position =
            value
                .toInt()
                .coerceAtLeast(0)

        _uiState.update {
            it.copy(
                currentPosition = position
            )
        }

        updateLyric(position)
    }

    /**
     * MediaController 完成 seek 后调用。
     */
    fun finishMedia3Seek(
        position: Int
    ) {
        val safePosition =
            position.coerceAtLeast(0)

        isUserSeeking =
            false

        _uiState.update {
            it.copy(
                currentPosition = safePosition
            )
        }

        updateLyric(safePosition)
    }

    fun updateMedia3Volume(
        value: Float
    ) {
        _uiState.update {
            it.copy(
                volume = value.coerceIn(
                    0f,
                    1f
                )
            )
        }
    }

    fun updateMedia3PlaybackSpeed(
        speed: Float
    ) {
        _uiState.update {
            it.copy(
                playbackSpeed =
                    speed.coerceIn(
                        0.5f,
                        2f
                    )
            )
        }
    }

    fun changePlayMode() {
        _uiState.update {
            it.copy(
                playMode =
                    it.playMode.nextMode()
            )
        }
    }

    /**
     * 点击歌词后查找歌词对应的时间。
     */
    fun findLyricPosition(
        lyricText: String
    ): Int? {
        return lyricLines
            .firstOrNull {
                it.text == lyricText
            }
            ?.timeMs
    }

    fun setPlaybackError(
        message: String
    ) {
        _uiState.update {
            it.copy(
                isPlaying = false,
                currentLyric = message
            )
        }
    }

    // ================= 收藏 =================

    fun toggleFavorite(
        song: Song
    ) {
        val favorites =
            _uiState.value.favoriteSongIds

        val newFavorites =
            if (favorites.contains(song.id)) {
                favorites - song.id
            } else {
                favorites + song.id
            }

        _uiState.update {
            it.copy(
                favoriteSongIds =
                    newFavorites
            )
        }
    }

    fun onFavoriteClick(
        song: Song
    ) {
        toggleFavorite(song)
    }

    // ================= 播放历史 =================

    private fun addToPlaybackHistory(
        song: Song
    ) {
        playbackHistory.removeAll {
            it.id == song.id
        }

        playbackHistory.add(
            0,
            song
        )

        if (playbackHistory.size >
            historyLimit
        ) {
            playbackHistory.removeAt(
                playbackHistory.lastIndex
            )
        }

        _uiState.update {
            it.copy(
                playbackHistory =
                    playbackHistory.toList()
            )
        }
    }

    fun clearPlaybackHistory() {
        playbackHistory.clear()

        _uiState.update {
            it.copy(
                playbackHistory =
                    emptyList()
            )
        }
    }

    fun removeSongFromHistory(
        song: Song
    ) {
        playbackHistory.removeAll {
            it.id == song.id
        }

        _uiState.update {
            it.copy(
                playbackHistory =
                    playbackHistory.toList()
            )
        }
    }

    // ================= 歌词 =================

    private fun loadSongLyrics(
        song: Song
    ) {
        loadLyrics(song)
    }


    private fun loadLyrics(
        song: Song
    ) {

        viewModelScope.launch {

            // 标记正在加载歌词
            isLyricsLoading = true

            _uiState.update {
                it.copy(
                    currentLyric = "等待歌词...",
                    nextLyric = "",
                    lyricWindow = emptyList(),
                    activeLyricIndex = -1,
                    fullLyricLines = emptyList(),
                    currentLyricIndex = -1,
                    isPlainLyrics = false
                )
            }

            try {

                /*
                 * ViewModel 不关心歌曲来自本地还是在线。
                 * Repository 会根据 song.source
                 * 自动选择对应的 MusicSource。
                 */
                val lyricContent =
                    repository.getLyrics(song)

                // 加载期间已经切歌，旧歌词直接丢弃
                if (
                    _uiState.value.currentSongId
                    != song.id
                ) {
                    return@launch
                }

                when (lyricContent) {

                    /*
                     * 带时间轴歌词
                     */
                    is LyricContent.Timed -> {

                        lyricLines =
                            lyricContent.lines

                        // ★ 很关键
                        isLyricsLoading = false

                        _uiState.update {
                            it.copy(
                                isPlainLyrics = false,
                                fullLyricLines = emptyList(),
                                currentLyricIndex = -1
                            )
                        }

                        /*
                         * 不要固定传 0，
                         * 使用播放器当前真实进度。
                         */
                        updateLyric(
                            _uiState.value.currentPosition
                        )
                    }

                    /*
                     * 普通文本歌词
                     */
                    is LyricContent.Plain -> {

                        lyricLines =
                            emptyList()

                        // ★ 加载结束
                        isLyricsLoading = false

                        _uiState.update {
                            it.copy(
                                currentLyric = "",
                                nextLyric = "",
                                lyricWindow = emptyList(),
                                activeLyricIndex = -1,
                                fullLyricLines =
                                    lyricContent.lines,
                                currentLyricIndex = -1,
                                isPlainLyrics = true
                            )
                        }
                    }

                    /*
                     * 无歌词
                     */
                    LyricContent.Empty -> {

                        lyricLines =
                            emptyList()

                        // ★ 加载结束
                        isLyricsLoading = false

                        showNoLyrics()
                    }
                }

            } catch (e: Exception) {

                Log.e(
                    "MusicViewModel",
                    "歌词加载失败：${song.name}",
                    e
                )

                // ★ 异常也必须结束 loading 状态
                isLyricsLoading = false

                if (
                    _uiState.value.currentSongId
                    == song.id
                ) {

                    lyricLines =
                        emptyList()

                    showNoLyrics()
                }
            }
        }
    }



    private fun showNoLyrics() {
        _uiState.update {
            it.copy(
                currentLyric = "暂无歌词",
                nextLyric = "",
                lyricWindow =
                    listOf("暂无歌词"),
                activeLyricIndex = 0,
                fullLyricLines =
                    emptyList(),
                currentLyricIndex = -1,
                isPlainLyrics = false
            )
        }
    }

    private fun updateLyric(
        position: Int
    ) {
        if (isLyricsLoading) {
            return
        }

        if (_uiState.value.isPlainLyrics) {
            return
        }

        if (lyricLines.isEmpty()) {
            showNoLyrics()
            return
        }

        val currentIndex =
            lyricLines.indexOfLast {
                position >= it.timeMs
            }

        val safeIndex =
            if (currentIndex >= 0) {
                currentIndex
            } else {
                0
            }

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

        _uiState.update {
            it.copy(
                currentLyric =
                    lyricLines[safeIndex].text,

                nextLyric =
                    lyricLines.getOrNull(
                        safeIndex + 1
                    )?.text ?: "",

                lyricWindow =
                    lyricWindow,

                activeLyricIndex =
                    safeIndex -
                            realStartIndex,

                fullLyricLines =
                    lyricLines.map {
                            line -> line.text
                    },

                currentLyricIndex =
                    safeIndex
            )
        }
    }

    // ================= 在线歌曲刷新 =================

    fun refreshOnlineSongs() {
        if (
            _uiState.value
                .isRefreshingOnlineSongs
        ) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRefreshingOnlineSongs =
                        true
                )
            }

            try {
                Log.d(
                    "MusicViewModel",
                    "开始刷新在线歌曲"
                )

                val newOnlineSongs =
                    repository.refreshSongs(
                        SongSource.ONLINE
                    )

                if (newOnlineSongs.isNotEmpty()) {
                    val localSongs =
                        songs.filter {
                            it.source !=
                                    SongSource.ONLINE
                        }

                    songs =
                        localSongs +
                                newOnlineSongs

                    _uiState.update {
                        val currentId =
                            if (
                                songs.any { song ->
                                    song.id ==
                                            it.currentSongId
                                }
                            ) {
                                it.currentSongId
                            } else {
                                songs.firstOrNull()
                                    ?.id ?: 0
                            }

                        it.copy(
                            songs = songs,
                            currentSongId =
                                currentId,
                            isRefreshingOnlineSongs =
                                false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isRefreshingOnlineSongs =
                                false
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
                        isRefreshingOnlineSongs =
                            false
                    )
                }
            }
        }
    }
}