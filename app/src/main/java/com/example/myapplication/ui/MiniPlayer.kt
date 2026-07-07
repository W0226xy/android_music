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
fun MiniPlayer(//compose组件
    uiState: MusicUiState,//当前播放器状态
    onMiniPlayerClick: () -> Unit,//点击迷你播放器时触发
    onPlayPauseClick: () -> Unit//点击播放-暂停按钮触发
) {
    val currentSong = uiState.currentSong ?: return//从 uiState 中取出当前歌曲如果当前歌曲为空，就直接返回，不显示 MiniPlayer

    Surface(//可以理解为一个带背景和主题的盒子(外层容器)
        shadowElevation = 8.dp,//排版设置
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onMiniPlayerClick()//用户点击底部迷你播放器调用函数（会展示详情播放页）
            }
    ) {
        Row(//横向布局
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