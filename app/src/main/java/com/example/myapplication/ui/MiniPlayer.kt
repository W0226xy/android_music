package com.example.myapplication.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.MusicUiState

@Composable
fun MiniPlayer(
    uiState: MusicUiState,
    onMiniPlayerClick: () -> Unit,
    onPlayPauseClick: () -> Unit
) {
    val currentSong = uiState.currentSong ?: return

    Surface(
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onMiniPlayerClick()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (uiState.isPlaying) "正在播放" else "当前歌曲",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "${currentSong.name} - ${currentSong.singer}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    onPlayPauseClick()
                }
            ) {
                Text(
                    text = if (uiState.isPlaying) "暂停" else "播放"
                )
            }
        }
    }
}