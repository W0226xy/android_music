package com.example.myapplication.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.myapplication.data.MusicUiState
import com.example.myapplication.data.Song
import com.example.myapplication.data.SongSource

// 定义应用页面状态
enum class AppScreen {
    MUSIC_LIST,
    ONLINE_MUSIC,
    PLAYER_DETAIL,
    PLAYBACK_HISTORY
}

@Composable
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
    onLyricClick: (String) -> Unit,
    onOnlineMusicClick: () -> Unit,
    onRefreshOnlineSongs: () -> Unit,
    onAddWidgetClick: () -> Unit,

    // 每次点击系统媒体通知时，这个值都会变化。
    openPlayerRequest: Int = 0
) {
    var currentScreen by rememberSaveable {
        mutableStateOf(
            AppScreen.MUSIC_LIST
        )
    }

    LaunchedEffect(
        openPlayerRequest
    ) {
        if (openPlayerRequest > 0) {
            currentScreen =
                AppScreen.PLAYER_DETAIL
        }
    }

    when (currentScreen) {
        AppScreen.MUSIC_LIST -> {
            MusicListScreen(
                uiState = uiState,
                onSearchTextChange =
                    onSearchTextChange,
                onSongClick =
                    onSongClick,
                onPlayClick =
                    onPlayClick,
                onFavoriteClick =
                    onFavoriteClick,
                onMiniPlayerClick = {
                    currentScreen =
                        AppScreen.PLAYER_DETAIL
                },
                onPlayPauseClick =
                    onPlayPauseClick,
                onPlayModeClick =
                    onPlayModeClick,
                onHistoryClick = {
                    currentScreen =
                        AppScreen.PLAYBACK_HISTORY
                },
                onOnlineMusicClick = {
                    onOnlineMusicClick()

                    currentScreen =
                        AppScreen.ONLINE_MUSIC
                },
                onAddWidgetClick = onAddWidgetClick
            )
        }

        AppScreen.PLAYER_DETAIL -> {
            PlayerDetailScreen(
                uiState = uiState,
                onBackClick = {
                    currentScreen =
                        when (
                            uiState
                                .currentSong
                                ?.source
                        ) {
                            SongSource.ONLINE ->
                                AppScreen.ONLINE_MUSIC

                            SongSource.LOCAL ->
                                AppScreen.MUSIC_LIST

                            null ->
                                AppScreen.MUSIC_LIST
                        }
                },
                onPlayPauseClick =
                    onPlayPauseClick,
                onPreviousClick =
                    onPreviousClick,
                onNextClick =
                    onNextClick,
                onSeekForwardClick =
                    onSeekForwardClick,
                onSeekBackwardClick =
                    onSeekBackwardClick,
                onProgressChange =
                    onProgressChange,
                onSeekFinished =
                    onSeekFinished,
                onVolumeChange =
                    onVolumeChange,
                onPlaybackSpeedChange =
                    onPlaybackSpeedChange,
                onLyricClick =
                    onLyricClick
            )
        }

        AppScreen.PLAYBACK_HISTORY -> {
            PlaybackHistoryScreen(
                playbackHistory =
                    uiState.playbackHistory,
                onSongClick = {
                        song ->
                    onSongClick(song)

                    currentScreen =
                        AppScreen.MUSIC_LIST
                },
                onClearHistory =
                    onClearPlaybackHistory,
                onRemoveSong =
                    onRemoveSongFromHistory,
                onBackClick = {
                    currentScreen =
                        AppScreen.MUSIC_LIST
                }
            )
        }

        AppScreen.ONLINE_MUSIC -> {
            OnlineMusicScreen(
                uiState = uiState,
                onSongClick =
                    onSongClick,
                onBackClick = {
                    currentScreen =
                        AppScreen.MUSIC_LIST
                },
                onFavoriteClick =
                    onFavoriteClick,
                onMiniPlayerClick = {
                    currentScreen =
                        AppScreen.PLAYER_DETAIL
                },
                onPlayPauseClick =
                    onPlayPauseClick,
                onMoreSongsClick =
                    onRefreshOnlineSongs,
                isRefreshing =
                    uiState
                        .isRefreshingOnlineSongs
            )
        }
    }
}