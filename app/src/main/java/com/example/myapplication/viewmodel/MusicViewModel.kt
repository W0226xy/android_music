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

//ViewModel，逻辑和状态管理:负责接收 View 层传来的事件，并执行对应业务逻辑，同时维护 UI 状态。
//播放歌曲
//暂停歌曲
//切换上一首 / 下一首
//切换播放模式
//更新播放进度
//调节音量
//解析并同步歌词
//收藏歌曲
//更新 MusicUiState
class MusicViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val repository = MusicRepository()

    private var mediaPlayer: MediaPlayer? = null//播放 / 暂停 / seek / 获取进度 / 获取时长

    private var lyricLines: List<LyricLine> = emptyList()//保存解析后的歌词：(timeMs, text)

    private var isUserSeeking: Boolean = false//是否用户在拖动进度条，用户拖动进度条时，自动更新把 UI 覆盖掉

    private val songs: List<Song> = repository.getSongs()//UI状态

    // 播放历史记录管理
    private val _playbackHistory = mutableListOf<Song>()
    private val historyLimit = 100 // 最多记录100首歌曲

    private val _uiState = MutableStateFlow(//_uiState 是一个 MusicUiState 对象的容器，它包含整个音乐播放器的所有 UI 状态：
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

    fun onSearchTextChange(text: String) {//更新ui状态
        _uiState.value = _uiState.value.copy(//.value拿到容器里的值，
            searchText = text//searchText搜索框文本
        )
    }

    fun playSong(song: Song) {
        mediaPlayer?.release()//（1）释放旧播放器

        val player = MediaPlayer.create(context, song.audioResId)//2）创建新播放器

        if (player == null) {
            _uiState.value = _uiState.value.copy(
                isPlaying = false
            )
            return
        }

        mediaPlayer = player

        loadLyrics(song)//3）加载歌词

        player.setVolume(//设置音量
            _uiState.value.volume,
            _uiState.value.volume
        )

        // 设置播放速度
        val playbackSpeed = _uiState.value.playbackSpeed
        player.setPlaybackParams(player.playbackParams.setSpeed(playbackSpeed))

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

        // 添加到播放历史
        addToPlaybackHistory(song)
    }

    fun playOrPause() {//播放暂停逻辑
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

    fun playNextSong() {//下一首（根据播放模式设置播放id）
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

    fun playPreviousSong() {//上一首，类似下一首逻辑
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

    fun changePlayMode() {//改变播放模式
        _uiState.value = _uiState.value.copy(
            playMode = _uiState.value.playMode.nextMode()
        )
    }

    fun changeVolume(value: Float) {//设置音量
        val newVolume = value.coerceIn(0f, 1f)

        mediaPlayer?.setVolume(newVolume, newVolume)

        _uiState.value = _uiState.value.copy(
            volume = newVolume
        )
    }

    fun changePlaybackSpeed(speed: Float) {//设置播放速度
        val newSpeed = speed.coerceIn(0.5f, 2.0f)

        mediaPlayer?.setPlaybackParams(
            mediaPlayer?.playbackParams?.setSpeed(newSpeed) ?: android.media.PlaybackParams().setSpeed(newSpeed)
        )

        _uiState.value = _uiState.value.copy(
            playbackSpeed = newSpeed
        )
    }

    fun onProgressChange(value: Float) {//用户拖动进度条
        isUserSeeking = true

        val newPosition = value.toInt()

        _uiState.value = _uiState.value.copy(
            currentPosition = newPosition
        )

        updateLyric(newPosition)
    }

    fun onSeekFinished() {//松手更新ui
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

    // ==================== 播放历史记录相关方法 ====================

    /**
     * 添加歌曲到播放历史
     * 如果歌曲已存在，会移除并添加到最前面
     */
    private fun addToPlaybackHistory(song: Song) {
        // 先移除已存在的相同歌曲
        _playbackHistory.removeAll { it.id == song.id }
        // 添加到最前面
        _playbackHistory.add(0, song)
        // 限制历史记录数量
        if (_playbackHistory.size > historyLimit) {
            _playbackHistory.removeAt(_playbackHistory.size - 1)
        }
        // 更新 UI 状态
        _uiState.value = _uiState.value.copy(
            playbackHistory = _playbackHistory.toList()
        )
    }

    /**
     * 清空播放历史
     */
    fun clearPlaybackHistory() {
        _playbackHistory.clear()
        _uiState.value = _uiState.value.copy(
            playbackHistory = emptyList()
        )
    }

    /**
     * 从历史记录中移除指定歌曲
     */
    fun removeSongFromHistory(song: Song) {
        _playbackHistory.removeAll { it.id == song.id }
        _uiState.value = _uiState.value.copy(
            playbackHistory = _playbackHistory.toList()
        )
    }

    // ==================== 播放历史记录相关方法结束 ====================

    private fun handleSongCompletion(song: Song) {//播放完成，先看播放模式，决定下一首播放id，更新播放状态
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

    private fun loadLyrics(song: Song) {//加载歌词
        lyricLines = LyricParser.parseLrc(context, song.lyricResId)
        updateLyric(0)
    }

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

        val safeCurrentIndex = if (currentIndex >= 0) {//还没到第一句歌词处理（比如第一句歌词在10s，当前是3s）
            currentIndex
        } else {
            0
        }

        //设置5行歌词（当前在唱的，前两句，后两句）
        val startIndex = (safeCurrentIndex - 2).coerceAtLeast(0)
        val endIndex = (startIndex + 4).coerceAtMost(lyricLines.lastIndex)
        val realStartIndex = (endIndex - 4).coerceAtLeast(0)

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
                            currentPosition = position,
                            duration = duration
                        )

                        updateLyric(position)//更新进度条位置
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