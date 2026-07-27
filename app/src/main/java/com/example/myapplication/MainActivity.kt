package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.myapplication.ui.AutoMusicApp
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.viewmodel.MusicViewModel

class MainActivity : ComponentActivity() {

    private val musicViewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {

            MyApplicationTheme {

                val uiState by musicViewModel.uiState.collectAsState()

                AutoMusicApp(
                    uiState = uiState,

                    onSearchTextChange = musicViewModel::onSearchTextChange,

                    onSongClick = musicViewModel::playSong,

                    onPlayClick = musicViewModel::playSong,

                    onPlayPauseClick = musicViewModel::playOrPause,

                    onPreviousClick = musicViewModel::playPreviousSong,

                    onNextClick = musicViewModel::playNextSong,

                    onSeekForwardClick = musicViewModel::seekForward10s,

                    onSeekBackwardClick = musicViewModel::seekBackward10s,

                    onProgressChange = musicViewModel::onProgressChange,

                    onSeekFinished = musicViewModel::onSeekFinished,

                    onVolumeChange = musicViewModel::changeVolume,

                    onPlaybackSpeedChange = musicViewModel::changePlaybackSpeed,

                    onPlayModeClick = musicViewModel::changePlayMode,

                    onFavoriteClick = musicViewModel::toggleFavorite,

                    onClearPlaybackHistory = musicViewModel::clearPlaybackHistory,

                    onRemoveSongFromHistory = musicViewModel::removeSongFromHistory,

                    onLyricClick = musicViewModel::onLyricClick,

                    onOnlineMusicClick = {
                        // 页面切换由 AutoMusicApp 内部处理
                    },


                    onRefreshOnlineSongs =
                        musicViewModel::refreshOnlineSongs
                )
            }
        }
    }
}