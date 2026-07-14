package com.example.myapplication.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

@Composable
fun PlayerDetailScreen(//Compose 页面函数。
    uiState: MusicUiState,
    onBackClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeekForwardClick: () -> Unit,
    onSeekBackwardClick: () -> Unit,
    onProgressChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onLyricClick: (String) -> Unit
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

            Spacer(modifier = Modifier.weight(0.5f))

            // 歌词滚动显示区域
            val lazyListState = rememberLazyListState()//管理 `LazyColumn`（歌词列表）的滚动状态，包括当前滚动位置等信息。
            //rememberLazyListState() 是 Jetpack Compose 中用于管理 LazyColumn 或 LazyRow 滚动状态的函数。
            //### 主要功能
            //1. __状态记忆__：使用 `remember` 机制，在重组过程中保持滚动状态不变
            //2. __滚动控制__：提供滚动位置、可见项信息、滚动动画等控制能力
            //3. __状态恢复__：支持配置变更（如屏幕旋转）时的状态恢复
            val coroutineScope = rememberCoroutineScope()//创建协程作用域，用于在 Compose 中安全地启动协程。
            //rememberCoroutineScope() 是 Jetpack Compose 中用于在可组合函数中安全启动协程的函数。
            //### 主要功能
            //1. __生命周期感知__：协程作用域与当前可组合项的生命周期绑定，当可组合项离开组合时自动取消
            //2. __安全启动__：在 Compose 副作用（如 LaunchedEffect、DisposableEffect）或事件回调中启动协程
            //3. __状态管理__：避免在可组合函数中直接使用 GlobalScope 或 ViewModel 协程作用域


            // 当前播放的歌词在完整列表中的位置
            val currentLyricIndex = uiState.currentLyricIndex

            // 当当前歌词索引变化且歌曲正在播放时，自动滚动到该位置
            LaunchedEffect(currentLyricIndex, uiState.isPlaying) {//LaunchedEffect这是一个 Compose 副作用函数，当 `currentLyricIndex` 或 `uiState.isPlaying` 发生变化时触发。
            //这是一个 Compose 副作用函数，当 `currentLyricIndex` 或 `uiState.isPlaying` 发生变化时触发。
                // 确保只有在依赖项变化时才执行滚动逻辑
                if (currentLyricIndex >= 0 && uiState.isPlaying) {
                    coroutineScope.launch {//启动一个协程
                        // 计算目标位置，使当前歌词在屏幕中央
                        val visibleItemsCount = 5 // 大致可见的项目数
                        val targetIndex = (currentLyricIndex - visibleItemsCount / 2).coerceAtLeast(0)
                        //将当前歌词索引减去可见歌词数的一半，使当前歌词位于屏幕中央。
                        //例如：如果当前歌词索引是 10，则 `10 - 5/2 = 10 - 2 = 8`，滚动到第 8 行，这样第 10 行歌词会显示在屏幕中央附近。
                        lazyListState.animateScrollToItem(//animateScrollToItem平滑滚动动画
                            index = targetIndex,
                            scrollOffset = 0
                        )
                    }
                }
            }

            // 歌词列表显示
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(3f)
                    .heightIn(max = 400.dp)
            ) {
                if (uiState.fullLyricLines.isNotEmpty()) {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(uiState.fullLyricLines) { lyric ->
                            val isActive = uiState.currentLyric.isNotEmpty() && lyric == uiState.currentLyric

                            Text(
                                text = lyric,
                                fontSize = if (isActive) 24.sp else 20.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color = if (isActive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .clickable {
                                        onLyricClick(lyric)
                                    }
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 12.dp)
                            )
                        }
                    }
                } else {
                    // 如果没有歌词，显示提示信息
                    Text(
                        text = "暂无歌词",
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

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

                Button(
                    onClick = {
                        val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f)
                        val currentIndex = speeds.indexOf(uiState.playbackSpeed)
                        val nextIndex = if (currentIndex == -1) 2 else (currentIndex + 1) % speeds.size
                        onPlaybackSpeedChange(speeds[nextIndex])
                    },
                    modifier = Modifier.size(50.dp),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Text(
                        "${uiState.playbackSpeed}x",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = onSeekBackwardClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.bg_backward),
                        contentDescription = "快退10秒",
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(0.5f))

                IconButton(
                    onClick = onPreviousClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.up),
                        contentDescription = "上一首",
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(60.dp)
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
                        modifier = Modifier.size(50.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = onNextClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.down),
                        contentDescription = "下一首",
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(0.5f))

                IconButton(
                    onClick = onSeekForwardClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.bg_forward),
                        contentDescription = "快进10秒",
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}