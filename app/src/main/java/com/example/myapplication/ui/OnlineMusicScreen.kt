package com.example.myapplication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.MusicUiState
import com.example.myapplication.data.Song
import com.example.myapplication.data.SongSource

@Composable
fun OnlineMusicScreen(
    uiState: MusicUiState,
    onSongClick: (Song) -> Unit,
    onBackClick: () -> Unit,
    onFavoriteClick: (Song) -> Unit,
    onMiniPlayerClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onMoreSongsClick: () -> Unit,
    isRefreshing: Boolean
) {

    val onlineSongs = uiState.songs.filter {
        it.source == SongSource.ONLINE
    }

    Scaffold(
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

            Button(
                onClick = onBackClick
            ) {
                Text("返回本地音乐")
            }

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 16.dp,
                        bottom = 12.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "在线音乐",
                    style = MaterialTheme.typography.titleLarge
                )

                Button(
                    onClick = onMoreSongsClick,
                    enabled = !isRefreshing
                ) {

                    if (isRefreshing) {

                        CircularProgressIndicator(
                            modifier = Modifier.width(18.dp),
                            strokeWidth = 2.dp
                        )

                        Spacer(
                            modifier = Modifier.width(8.dp)
                        )

                        Text("正在刷新")

                    } else {

                        Text("更多歌曲")
                    }
                }
            }

            if (onlineSongs.isEmpty()) {

                Text(
                    text = if (isRefreshing) {
                        "正在加载在线歌曲……"
                    } else {
                        "暂无在线歌曲"
                    },
                    modifier = Modifier.padding(top = 16.dp)
                )

            } else {

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {

                    items(
                        items = onlineSongs,
                        key = { song ->
                            song.id
                        }
                    ) { song ->

                        val isCurrentSong =
                            song.id == uiState.currentSongId

                        val isFavorite =
                            uiState.favoriteSongIds.contains(song.id)

                        SongItem(
                            song = song,

                            isCurrentSong = isCurrentSong,

                            isPlaying =
                                isCurrentSong && uiState.isPlaying,

                            isFavorite = isFavorite,

                            onSongClick = {
                                onSongClick(song)
                            },

                            onPlayClick = {

                                if (
                                    isCurrentSong &&
                                    uiState.isPlaying
                                ) {
                                    onPlayPauseClick()
                                } else {
                                    onSongClick(song)
                                }
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
}