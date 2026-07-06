package com.example.myapplication.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.data.MusicUiState
import com.example.myapplication.utils.formatTime

@Composable
fun PlayerDetailScreen(//Compose 页面函数。
    uiState: MusicUiState,
    onBackClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onProgressChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onVolumeChange: (Float) -> Unit
) {
    val currentSong = uiState.currentSong ?: return//从 uiState 中获取当前歌曲,如果当前歌曲为空，就直接返回，不显示页面

    val maxProgress = if (uiState.duration > 0) {//计算进度条最大值(歌曲总时常)
        uiState.duration.toFloat()
    } else {
        1f
    }

    val progress = uiState.currentPosition//计算当前播放进度
        .coerceIn(0, if (uiState.duration > 0) uiState.duration else 1)
        .toFloat()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Button(
                onClick = onBackClick
            ) {
                Text("返回")
            }

            Text(
                text = currentSong.name,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 24.dp)
            )

            Text(
                text = currentSong.singer,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )

            Text(
                text = currentSong.album,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                uiState.lyricWindow.forEachIndexed { index, lyric ->
                    val isActive = index == uiState.activeLyricIndex

                    Text(
                        text = lyric,
                        fontSize = if (isActive) 22.sp else 15.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Slider(
                value = progress,
                onValueChange = onProgressChange,
                onValueChangeFinished = onSeekFinished,
                valueRange = 0f..maxProgress,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = formatTime(uiState.currentPosition),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = formatTime(uiState.duration),
                    fontSize = 13.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text(
                    text = "音量",
                    fontSize = 14.sp
                )

                Slider(
                    value = uiState.volume,
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = onPreviousClick,
                    modifier = Modifier.size(64.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.up),
                        contentDescription = "上一首",
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(76.dp)
                ) {
                    Image(
                        painter = painterResource(
                            id = if (uiState.isPlaying) {
                                R.drawable.play_on
                            } else {
                                R.drawable.play_2
                            }
                        ),
                        contentDescription = if (uiState.isPlaying) "暂停" else "播放",
                        modifier = Modifier.size(60.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = onNextClick,
                    modifier = Modifier.size(64.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.down),
                        contentDescription = "下一首",
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}