package com.example.myapplication.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.Song

@Composable
fun SongItem(//单个歌曲条目组件（显示歌曲名、专辑、歌手、当前是否正在播放，是否喜欢）
    song: Song,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onSongClick: () -> Unit,//点击按钮后触发事件
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable {
                onSongClick()
            },
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Text(
                    text = "${song.singer} · ${song.album}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                if (isCurrentSong) {
                    Text(
                        text = if (isPlaying) "正在播放" else "当前歌曲",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            TextButton(
                onClick = onFavoriteClick
            ) {
                Text(
                    text = if (isFavorite) "♥" else "♡",
                    fontSize = 24.sp
                )
            }

            Button(
                onClick = onPlayClick
            ) {
                Text(
                    text = if (isCurrentSong && isPlaying) "暂停" else "播放"
                )
            }
        }
    }
}