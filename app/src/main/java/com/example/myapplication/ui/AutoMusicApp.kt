package com.example.myapplication.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.myapplication.data.MusicUiState
import com.example.myapplication.data.Song

// 定义应用页面状态
enum class AppScreen {
    MUSIC_LIST,      // 歌曲列表页
    PLAYER_DETAIL,   // 播放详情页
    PLAYBACK_HISTORY // 播放历史页
}

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
    onSeekForwardClick: () -> Unit,
    onSeekBackwardClick: () -> Unit,
    onProgressChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onPlayModeClick: () -> Unit,
    onFavoriteClick: (Song) -> Unit,
    onClearPlaybackHistory: () -> Unit,
    onRemoveSongFromHistory: (Song) -> Unit,
    onLyricClick: (String) -> Unit
) {
    var currentScreen by rememberSaveable {
        mutableStateOf(AppScreen.MUSIC_LIST)
    }

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
    }
}