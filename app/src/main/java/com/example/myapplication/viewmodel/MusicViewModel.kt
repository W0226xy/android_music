package com.example.myapplication.viewmodel

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.LyricLine
import com.example.myapplication.data.MusicUiState
import com.example.myapplication.data.PlayMode
import com.example.myapplication.data.Song
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

    private var isUserSeeking: Boolean = false

    private val songs: List<Song> = repository.getSongs()

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

        startProgressLoop()
    }

    fun onSearchTextChange(text: String) {
        _uiState.value = _uiState.value.copy(
            searchText = text
        )
    }

    fun playSong(song: Song) {
        mediaPlayer?.release()

        val player = MediaPlayer.create(context, song.audioResId)

        if (player == null) {
            _uiState.value = _uiState.value.copy(
                isPlaying = false
            )
            return
        }

        mediaPlayer = player

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
            duration = player.duration
        )
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

        val nextSong = if (state.playMode == PlayMode.SHUFFLE && songs.size > 1) {
            songs.filter { it.id != state.currentSongId }.random()
        } else {
            val currentIndex = songs.indexOfFirst { it.id == state.currentSongId }
            val nextIndex = if (currentIndex == -1) {
                0
            } else {
                (currentIndex + 1) % songs.size
            }

            songs[nextIndex]
        }

        playSong(nextSong)
    }

    fun playPreviousSong() {
        val state = _uiState.value

        val previousSong = if (state.playMode == PlayMode.SHUFFLE && songs.size > 1) {
            songs.filter { it.id != state.currentSongId }.random()
        } else {
            val currentIndex = songs.indexOfFirst { it.id == state.currentSongId }
            val previousIndex = if (currentIndex <= 0) {
                songs.lastIndex
            } else {
                currentIndex - 1
            }

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
        val newVolume = value.coerceIn(0f, 1f)

        mediaPlayer?.setVolume(newVolume, newVolume)

        _uiState.value = _uiState.value.copy(
            volume = newVolume
        )
    }

    fun onProgressChange(value: Float) {
        isUserSeeking = true

        val newPosition = value.toInt()

        _uiState.value = _uiState.value.copy(
            currentPosition = newPosition
        )

        updateLyric(newPosition)
    }

    fun onSeekFinished() {
        val position = _uiState.value.currentPosition

        mediaPlayer?.seekTo(position.coerceAtLeast(0))

        isUserSeeking = false
    }

    fun toggleFavorite(song: Song) {
        val state = _uiState.value
        val oldFavorites = state.favoriteSongIds

        val newFavorites = if (oldFavorites.contains(song.id)) {
            oldFavorites - song.id
        } else {
            oldFavorites + song.id
        }

        _uiState.value = state.copy(
            favoriteSongIds = newFavorites
        )
    }

    private fun handleSongCompletion(song: Song) {
        val state = _uiState.value

        when (state.playMode) {
            PlayMode.SINGLE_LOOP -> {
                mediaPlayer?.seekTo(0)
                mediaPlayer?.start()

                _uiState.value = state.copy(
                    currentPosition = 0,
                    isPlaying = true
                )
            }

            PlayMode.LIST_LOOP -> {
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

                playSong(nextSong)
            }
        }
    }

    private fun loadLyrics(song: Song) {
        lyricLines = LyricParser.parseLrc(context, song.lyricResId)
        updateLyric(0)
    }

    private fun updateLyric(position: Int) {
        if (lyricLines.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                currentLyric = "暂无歌词",
                nextLyric = "",
                lyricWindow = listOf("暂无歌词"),
                activeLyricIndex = 0
            )
            return
        }

        val currentIndex = lyricLines.indexOfLast {
            position >= it.timeMs
        }

        val safeCurrentIndex = if (currentIndex >= 0) {
            currentIndex
        } else {
            0
        }

        val startIndex = (safeCurrentIndex - 2).coerceAtLeast(0)
        val endIndex = (startIndex + 4).coerceAtMost(lyricLines.lastIndex)
        val realStartIndex = (endIndex - 4).coerceAtLeast(0)

        val lyricWindow = lyricLines
            .subList(realStartIndex, endIndex + 1)
            .map { it.text }

        val activeIndex = safeCurrentIndex - realStartIndex

        val currentLyric = if (currentIndex >= 0) {
            lyricLines[currentIndex].text
        } else {
            "等待歌词..."
        }

        val nextLyric = if (currentIndex + 1 in lyricLines.indices) {
            lyricLines[currentIndex + 1].text
        } else {
            ""
        }

        _uiState.value = _uiState.value.copy(
            currentLyric = currentLyric,
            nextLyric = nextLyric,
            lyricWindow = lyricWindow,
            activeLyricIndex = activeIndex
        )
    }

    private fun startProgressLoop() {
        viewModelScope.launch {
            while (isActive) {
                val player = mediaPlayer

                if (player != null) {
                    val position = player.currentPosition
                    val duration = player.duration

                    if (!isUserSeeking) {
                        _uiState.value = _uiState.value.copy(
                            currentPosition = position,
                            duration = duration
                        )

                        updateLyric(position)
                    }
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