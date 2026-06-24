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

@Composable
fun MusicListScreen(
    uiState: MusicUiState,
    onSearchTextChange: (String) -> Unit,
    onSongClick: (Song) -> Unit,
    onPlayClick: (Song) -> Unit,
    onFavoriteClick: (Song) -> Unit,
    onMiniPlayerClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onPlayModeClick: () -> Unit
) {
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

            TextField(
                value = uiState.searchText,
                onValueChange = onSearchTextChange,
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
            }

            LazyColumn {
                items(
                    items = uiState.filteredSongs,
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