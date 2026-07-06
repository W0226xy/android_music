package com.example.myapplication.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.myapplication.data.MusicUiState
import com.example.myapplication.data.Song

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
    onFavoriteClick: (Song) -> Unit
) {
    var showPlayerDetail by rememberSaveable {
        mutableStateOf(false)
    }

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
    }
}