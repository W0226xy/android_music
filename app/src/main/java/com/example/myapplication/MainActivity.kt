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
                val uiState by musicViewModel.uiState.collectAsState()//让 Compose “监听” ViewModel 里的状态变化
                //1.collectAsState() 会订阅 musicViewModel.uiState,相当于ui监听viewModel变化
                //2.StateFlow 发出新值
                //3.collectAsState 监听到 value 改变
                //4.Compose 触发重组

                //触发重组后会执行@Composable的函数
                //这里不是所有@Composable 函数都会重新执行，重组只会重新执行“读取了发生变化的 State 的 Composable 函数
                AutoMusicApp(
                    uiState = uiState,
                    onSearchTextChange = musicViewModel::onSearchTextChange,
                    onSongClick = musicViewModel::playSong,
                    onPlayClick = musicViewModel::playSong,
                    onPlayPauseClick = musicViewModel::playOrPause,
                    onPreviousClick = musicViewModel::playPreviousSong,
                    onNextClick = musicViewModel::playNextSong,
                    onProgressChange = musicViewModel::onProgressChange,
                    onSeekFinished = musicViewModel::onSeekFinished,
                    onVolumeChange = musicViewModel::changeVolume,
                    onPlaybackSpeedChange = musicViewModel::changePlaybackSpeed,
                    onPlayModeClick = musicViewModel::changePlayMode,
                    onFavoriteClick = musicViewModel::toggleFavorite,
                    onClearPlaybackHistory = musicViewModel::clearPlaybackHistory,
                    onRemoveSongFromHistory = musicViewModel::removeSongFromHistory,
                    onLyricClick = musicViewModel::onLyricClick
                )
            }
        }
    }
}