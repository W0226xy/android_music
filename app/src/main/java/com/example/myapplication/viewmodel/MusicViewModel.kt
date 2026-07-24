package com.example.myapplication.viewmodel

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

<<<<<<< HEAD
    private val songs: List<Song> = repository.getSongs()//UI状态

    private val _uiState = MutableStateFlow(
        MusicUiState(
            songs = songs,
            currentSongId = songs.firstOrNull()?.id ?: 0
        )
    )

    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    init {
        songs.firstOrNull()?.let { firstSong ->
            loadLyrics(firstSong)
        }

=======
    private val _uiState = MutableStateFlow(
        MusicUiState()
    )

    val uiState: StateFlow<MusicUiState> =
        _uiState.asStateFlow()

    init {
        loadSongs()
>>>>>>> 1f3ec53 (增加在线音乐服务器功能，目前服务器测试歌曲可被正确读取到客户端)
        startProgressLoop()
    }

    /**
     * 加载本地歌曲 + 服务器歌曲
     */
    private fun loadSongs() {
        viewModelScope.launch {

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

        mediaPlayer?.release()

        val player = MediaPlayer()
        mediaPlayer = player

        try {

            if (song.source == SongSource.LOCAL) {

                val uri = Uri.parse(
                    "android.resource://${context.packageName}/${song.audioResId}"
                )

                player.setDataSource(
                    context,
                    uri
                )

                player.prepare()

                loadLyrics(song)

                player.setVolume(
                    _uiState.value.volume,
                    _uiState.value.volume
                )

                player.setOnCompletionListener {
                    handleSongCompletion(song)
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

                val url = song.url ?: return

                Log.d("MusicViewModel", "开始播放在线歌曲：$url")

                player.setDataSource(url)

                player.setVolume(
                    _uiState.value.volume,
                    _uiState.value.volume
                )

                player.setOnPreparedListener {

                    Log.d("MusicViewModel", "网络歌曲准备完成")

                    loadLyrics(song)

                    it.start()
                    Log.d(
                        "MusicViewModel",
                        "开始播放，duration=${it.duration}"
                    )

                    _uiState.value = _uiState.value.copy(
                        currentSongId = song.id,
                        isPlaying = true,
                        currentPosition = 0,
                        duration = it.duration,
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
                        "MediaPlayer error what=$what extra=$extra"
                    )

                    true
                }

                player.prepareAsync()
            }

        } catch (e: Exception) {

            Log.e(
                "MusicViewModel",
                "playSong error",
                e
            )

            _uiState.value = _uiState.value.copy(
                isPlaying = false
            )
        }
<<<<<<< HEAD

        mediaPlayer = player

        loadLyrics(song)//3）加载歌词

        player.setVolume(//设置音量
            _uiState.value.volume,
            _uiState.value.volume
        )

        player.setOnCompletionListener {//播放完成监听，播完自动下一首 / 循环 / 单曲循环
            handleSongCompletion(song)
        }

        player.start()//开始播放

        _uiState.value = _uiState.value.copy(//更新ui状态
            currentSongId = song.id,
            isPlaying = true,
            currentPosition = 0,
            duration = player.duration
        )
=======
>>>>>>> 1f3ec53 (增加在线音乐服务器功能，目前服务器测试歌曲可被正确读取到客户端)
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
<<<<<<< HEAD
            if (player == null) {
                playSong(currentSong)
            } else {
=======

            if (player == null) {

                playSong(currentSong)

            } else {

>>>>>>> 1f3ec53 (增加在线音乐服务器功能，目前服务器测试歌曲可被正确读取到客户端)
                player.start()

                _uiState.value = state.copy(
                    isPlaying = true
                )
            }
        }
    }


    fun playNextSong() {

        val state = _uiState.value

<<<<<<< HEAD
        val nextSong = if (state.playMode == PlayMode.SHUFFLE && songs.size > 1) {
            songs.filter { it.id != state.currentSongId }.random()
        } else {
            val currentIndex = songs.indexOfFirst { it.id == state.currentSongId }
            val nextIndex = if (currentIndex == -1) {
                0
            } else {
                (currentIndex + 1) % songs.size
            }
=======
        val nextSong =
            if (state.playMode == PlayMode.SHUFFLE && songs.size > 1) {
>>>>>>> 1f3ec53 (增加在线音乐服务器功能，目前服务器测试歌曲可被正确读取到客户端)

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

<<<<<<< HEAD
    fun changeVolume(value: Float) {//设置音量
        val newVolume = value.coerceIn(0f, 1f)
=======
>>>>>>> 1f3ec53 (增加在线音乐服务器功能，目前服务器测试歌曲可被正确读取到客户端)

    fun changeVolume(value: Float) {

        val volume = value.coerceIn(0f, 1f)

<<<<<<< HEAD
    fun onProgressChange(value: Float) {//用户拖动进度条
=======
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

>>>>>>> 1f3ec53 (增加在线音乐服务器功能，目前服务器测试歌曲可被正确读取到客户端)
        isUserSeeking = true

        val position = value.toInt()

        _uiState.value = _uiState.value.copy(
<<<<<<< HEAD
            currentPosition = newPosition
        )

        updateLyric(newPosition)
=======
            currentPosition = position
        )

        updateLyric(position)
>>>>>>> 1f3ec53 (增加在线音乐服务器功能，目前服务器测试歌曲可被正确读取到客户端)
    }


<<<<<<< HEAD
        mediaPlayer?.seekTo(position.coerceAtLeast(0))
=======
    fun onSeekFinished() {

        val position =
            _uiState.value.currentPosition

        mediaPlayer?.seekTo(
            position.coerceAtLeast(0)
        )
>>>>>>> 1f3ec53 (增加在线音乐服务器功能，目前服务器测试歌曲可被正确读取到客户端)

        isUserSeeking = false
    }

<<<<<<< HEAD
    fun toggleFavorite(song: Song) {
        val state = _uiState.value
        val oldFavorites = state.favoriteSongIds
=======

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

>>>>>>> 1f3ec53 (增加在线音乐服务器功能，目前服务器测试歌曲可被正确读取到客户端)

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

<<<<<<< HEAD
    private fun handleSongCompletion(song: Song) {//播放完成，先看播放模式，决定下一首播放id，更新播放状态
        val state = _uiState.value
=======

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
>>>>>>> 1f3ec53 (增加在线音乐服务器功能，目前服务器测试歌曲可被正确读取到客户端)

            PlayMode.SINGLE_LOOP -> {

                mediaPlayer?.seekTo(0)
                mediaPlayer?.start()

<<<<<<< HEAD
                _uiState.value = state.copy(
                    currentPosition = 0,
                    isPlaying = true
                )
=======
                _uiState.value =
                    _uiState.value.copy(
                        currentPosition = 0,
                        isPlaying = true
                    )
>>>>>>> 1f3ec53 (增加在线音乐服务器功能，目前服务器测试歌曲可被正确读取到客户端)
            }


            PlayMode.LIST_LOOP -> {
<<<<<<< HEAD
                val currentIndex = songs.indexOfFirst { it.id == song.id }
                val nextIndex = if (currentIndex == -1) {
                    0
                } else {
                    (currentIndex + 1) % songs.size
                }

                playSong(songs[nextIndex])
            }

            PlayMode.SHUFFLE -> {
                val nextSong = if (songs.size == 1) {
                    song
                } else {
                    songs.filter { it.id != song.id }.random()
                }
=======

                playNextSong()

            }

>>>>>>> 1f3ec53 (增加在线音乐服务器功能，目前服务器测试歌曲可被正确读取到客户端)

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

<<<<<<< HEAD
    private fun updateLyric(position: Int) {//更新歌词
        if (lyricLines.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                currentLyric = "暂无歌词",
                nextLyric = "",
                lyricWindow = listOf("暂无歌词"),
                activeLyricIndex = 0
            )
            return
        }

        val currentIndex = lyricLines.indexOfLast {//从 lyricLines 中找到最后一个 timeMs 小于等于当前播放进度 position 的歌词下标
            position >= it.timeMs
        }
=======
    private fun updateLyric(position: Int) {

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

>>>>>>> 1f3ec53 (增加在线音乐服务器功能，目前服务器测试歌曲可被正确读取到客户端)

        val currentIndex =
            lyricLines.indexOfLast {
                position >= it.timeMs
            }


<<<<<<< HEAD
        val lyricWindow = lyricLines//歌词窗口
            .subList(realStartIndex, endIndex + 1)
            .map { it.text }

        val activeIndex = safeCurrentIndex - realStartIndex//计算当前高亮歌词在窗口中的位置（正在唱的歌词）

        val currentLyric = if (currentIndex >= 0) {//计算当前歌词文本
            lyricLines[currentIndex].text
        } else {
            "等待歌词..."
        }

        val nextLyric = if (currentIndex + 1 in lyricLines.indices) {
            lyricLines[currentIndex + 1].text
        } else {
            ""
        }

        _uiState.value = _uiState.value.copy(//更新ui状态
            currentLyric = currentLyric,
            nextLyric = nextLyric,
            lyricWindow = lyricWindow,
            activeLyricIndex = activeIndex
        )
    }

    private fun startProgressLoop() {//进度条随着时间更新，每隔 500 毫秒，从 MediaPlayer 获取当前播放进度和总时长， 然后更新到 MusicUiState 中，同时更新歌词。
        viewModelScope.launch {//开启一个后台循环任务，不阻塞主线程
            while (isActive) {
                val player = mediaPlayer

                if (player != null) {
                    val position = player.currentPosition
                    val duration = player.duration

                    if (!isUserSeeking) {//防止当前用户在拖进度条
                        _uiState.value = _uiState.value.copy(
=======
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
>>>>>>> 1f3ec53 (增加在线音乐服务器功能，目前服务器测试歌曲可被正确读取到客户端)
                            currentPosition = position,
                            duration = player.duration
                        )

<<<<<<< HEAD
                        updateLyric(position)//更新进度条位置
                    }
=======
                    updateLyric(position)
>>>>>>> 1f3ec53 (增加在线音乐服务器功能，目前服务器测试歌曲可被正确读取到客户端)
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
    }