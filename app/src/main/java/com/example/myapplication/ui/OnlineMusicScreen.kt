package com.example.myapplication.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    onPlayPauseClick: () -> Unit
) {

    val onlineSongs =
        uiState.songs.filter {
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


            Text(
                text = "在线音乐"
            )


            LazyColumn {


                items(
                    items = onlineSongs,
                    key = {
                        it.id
                    }
                ) { song ->


                    val isCurrentSong =
                        song.id == uiState.currentSongId


                    val isFavorite =
                        uiState.favoriteSongIds.contains(song.id)


                    SongItem(
                        song = song,

                        isCurrentSong = isCurrentSong,

                        isPlaying = uiState.isPlaying,

                        isFavorite = isFavorite,


                        onSongClick = {
                            onSongClick(song)
                        },


                        onPlayClick = {

                            if (isCurrentSong && uiState.isPlaying) {

                                // 当前歌曲正在播放，点击暂停
                                onPlayPauseClick()

                            } else {

                                // 播放新的在线歌曲
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