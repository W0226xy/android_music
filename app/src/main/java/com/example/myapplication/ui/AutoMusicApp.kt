package com.example.myapplication.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.myapplication.data.MusicUiState
import com.example.myapplication.data.Song

@Composable
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
    onFavoriteClick: (Song) -> Unit
) {
    var showPlayerDetail by rememberSaveable {
        mutableStateOf(false)
    }

    if (showPlayerDetail) {
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
            onSearchTextChange = onSearchTextChange,
            onSongClick = onSongClick,
            onPlayClick = onPlayClick,
            onFavoriteClick = onFavoriteClick,
            onMiniPlayerClick = {
                showPlayerDetail = true
            },
            onPlayPauseClick = onPlayPauseClick,
            onPlayModeClick = onPlayModeClick
        )
    }
}