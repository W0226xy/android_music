package com.example.myapplication.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.myapplication.data.MusicUiState
import com.example.myapplication.data.Song
<<<<<<< HEAD
=======
import com.example.myapplication.data.SongSource
// 定义应用页面状态
enum class AppScreen {
    MUSIC_LIST,          // 本地歌曲列表
    ONLINE_MUSIC,        // 在线歌曲列表
    PLAYER_DETAIL,   // 播放详情页
    PLAYBACK_HISTORY // 播放历史页
}
>>>>>>> 1f3ec53 (增加在线音乐服务器功能，目前服务器测试歌曲可被正确读取到客户端)

//View 层只做两件事：显示 uiState
//把用户操作通过 onXXX 回调传出去

@Composable//Compose 的核心思想：状态变了，界面自动刷新
fun AutoMusicApp(
    uiState: MusicUiState,
    onSearchTextChange: (String) -> Unit,
    onSongClick: (Song) -> Unit,
    onPlayClick: (Song) -> Unit,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onProgressChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onPlayModeClick: () -> Unit,
<<<<<<< HEAD
    onFavoriteClick: (Song) -> Unit
=======
    onFavoriteClick: (Song) -> Unit,
    onClearPlaybackHistory: () -> Unit,
    onRemoveSongFromHistory: (Song) -> Unit,
    onLyricClick: (String) -> Unit,
    onOnlineMusicClick: () -> Unit
>>>>>>> 1f3ec53 (增加在线音乐服务器功能，目前服务器测试歌曲可被正确读取到客户端)
) {
    var showPlayerDetail by rememberSaveable {
        mutableStateOf(false)
    }

<<<<<<< HEAD
    if (showPlayerDetail) {//然后根据内部的 showPlayerDetail 状态决定显示歌曲列表页还是播放详情页。
        PlayerDetailScreen(
            uiState = uiState,
            onBackClick = {
                showPlayerDetail = false
            },
            onPlayPauseClick = onPlayPauseClick,
            onPreviousClick = onPreviousClick,
            onNextClick = onNextClick,
            onProgressChange = onProgressChange,
            onSeekFinished = onSeekFinished,
            onVolumeChange = onVolumeChange
        )
    } else {
        MusicListScreen(
            uiState = uiState,
            onSearchTextChange = onSearchTextChange,//onSearchTextChange = musicViewModel::onSearchTextChange函数转发
            onSongClick = onSongClick,
            onPlayClick = onPlayClick,
            onFavoriteClick = onFavoriteClick,
            onMiniPlayerClick = {
                showPlayerDetail = true
            },
            onPlayPauseClick = onPlayPauseClick,
            onPlayModeClick = onPlayModeClick
        )
=======
    // 根据当前页面状态决定显示哪个界面
    when (currentScreen) {
        AppScreen.MUSIC_LIST -> {
            MusicListScreen(
                uiState = uiState,
                onSearchTextChange = onSearchTextChange,
                onSongClick = onSongClick,
                onPlayClick = onPlayClick,
                onFavoriteClick = onFavoriteClick,
                onMiniPlayerClick = {
                    currentScreen = AppScreen.PLAYER_DETAIL
                },
                onPlayPauseClick = onPlayPauseClick,
                onPlayModeClick = onPlayModeClick,
                onHistoryClick = {
                    currentScreen = AppScreen.PLAYBACK_HISTORY
                },
                onOnlineMusicClick = {
                    currentScreen = AppScreen.ONLINE_MUSIC
                }
            )
        }

        AppScreen.PLAYER_DETAIL -> {
            PlayerDetailScreen(
                uiState = uiState,
                onBackClick = {
                    currentScreen = AppScreen.MUSIC_LIST
                },
                onPlayPauseClick = onPlayPauseClick,
                onPreviousClick = onPreviousClick,
                onNextClick = onNextClick,
                onSeekForwardClick = onSeekForwardClick,
                onSeekBackwardClick = onSeekBackwardClick,
                onProgressChange = onProgressChange,
                onSeekFinished = onSeekFinished,
                onVolumeChange = onVolumeChange,
                onPlaybackSpeedChange = onPlaybackSpeedChange,
                onLyricClick = onLyricClick
            )
        }

        AppScreen.PLAYBACK_HISTORY -> {
            PlaybackHistoryScreen(
                playbackHistory = uiState.playbackHistory,
                onSongClick = { song ->
                    onSongClick(song)
                    currentScreen = AppScreen.MUSIC_LIST
                },
                onClearHistory = onClearPlaybackHistory,
                onRemoveSong = onRemoveSongFromHistory,
                onBackClick = {
                    currentScreen = AppScreen.MUSIC_LIST
                }
            )
        }

        AppScreen.ONLINE_MUSIC -> {
            OnlineMusicScreen(
                uiState = uiState,
                onSongClick = onSongClick,
                onBackClick = {
                    currentScreen = AppScreen.MUSIC_LIST
                },
                onFavoriteClick = onFavoriteClick,
                onMiniPlayerClick = {
                    currentScreen = AppScreen.PLAYER_DETAIL
                },
                onPlayPauseClick = onPlayPauseClick
            )
        }
>>>>>>> 1f3ec53 (增加在线音乐服务器功能，目前服务器测试歌曲可被正确读取到客户端)
    }
}