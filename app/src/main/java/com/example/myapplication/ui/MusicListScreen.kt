package com.example.myapplication.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.MusicUiState
import com.example.myapplication.data.Song

@Composable//MusicListScreen = 一个“显示音乐列表的界面”，uiState 决定显示什么，onXXX 决定用户点击后做什么。
fun MusicListScreen(//Compose 页面函数,1. uiState：界面显示所需的状态数据 2. onXXX：用户操作时触发的回调函数
    uiState: MusicUiState,
    onSearchTextChange: (String) -> Unit,//搜索框内容变化时调用。
    onSongClick: (Song) -> Unit,//点击某首歌曲条目时调用。
    onPlayClick: (Song) -> Unit,//点击某首歌的播放按钮时调用，用来播放指定歌曲。
    onFavoriteClick: (Song) -> Unit,//点击收藏按钮时调用，用来收藏或取消收藏歌曲。
    onMiniPlayerClick: () -> Unit,//点击底部迷你播放器时调用，一般用于进入播放详情页。
    onPlayPauseClick: () -> Unit,//点击底部迷你播放器里的播放 / 暂停按钮时调用。
    onPlayModeClick: () -> Unit,//点击播放模式按钮时调用
    onHistoryClick: () -> Unit//点击播放历史按钮时调用
) {
    Scaffold(//标准页面布局容器
        bottomBar = {
            MiniPlayer(
                uiState = uiState,
                onMiniPlayerClick = onMiniPlayerClick,
                onPlayPauseClick = onPlayPauseClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "AutoMusic",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "基于 Jetpack Compose 的车机音乐播放器",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            TextField(//TextField输入框组件，参数value、onValueChange
                value = uiState.searchText,//获取搜索框内容
                onValueChange = onSearchTextChange,//用户输入时触发事件
                label = {
                    Text("搜索歌曲或歌手")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    text = "歌曲列表",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onPlayModeClick
                ) {
                    Text(uiState.playMode.label)
                }

                Spacer(modifier = Modifier.padding(start = 8.dp))

                Button(
                    onClick = onHistoryClick
                ) {
                    Text("播放历史")
                }
            }

            LazyColumn {//一个可滚动的垂直列表
                items(
                    items = uiState.filteredSongs,//返回被过滤后的若干歌曲
                    key = { it.id }
                ) { song ->
                    val isCurrentSong = song.id == uiState.currentSongId
                    val isFavorite = uiState.favoriteSongIds.contains(song.id)

                    SongItem(
                        song = song,
                        isCurrentSong = isCurrentSong,
                        isPlaying = uiState.isPlaying,
                        isFavorite = isFavorite,
                        onSongClick = {
                            onSongClick(song)
                        },
                        onPlayClick = {
                            onPlayClick(song)
                        },
                        onFavoriteClick = {
                            onFavoriteClick(song)
                        }
                    )
                }
            }
        }
    }
}