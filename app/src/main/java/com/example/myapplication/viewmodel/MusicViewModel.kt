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
    }