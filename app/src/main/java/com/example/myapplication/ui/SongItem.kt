package com.example.myapplication.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.MaterialTheme
import coil.compose.AsyncImage
import com.example.myapplication.data.Song
import com.example.myapplication.data.SongSource


@Composable
fun SongItem(
    song: Song,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onSongClick: () -> Unit,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 4.dp
            )
            .clickable {
                onSongClick()
            },

        shape = RoundedCornerShape(12.dp),

        tonalElevation = 2.dp
    ) {


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),

            verticalAlignment = Alignment.CenterVertically
        ) {


            // 在线歌曲显示封面
            if (
                song.source == SongSource.ONLINE &&
                song.coverUrl != null
            ) {

                AsyncImage(

                    model = song.coverUrl,

                    contentDescription = "歌曲封面",

                    modifier = Modifier
                        .size(56.dp)
                        .clip(
                            RoundedCornerShape(8.dp)
                        )
                )

            }


            Spacer(
                modifier = Modifier.width(12.dp)
            )


            Column(
                modifier = Modifier.weight(1f)
            ) {


                Text(

                    text = song.name,

                    fontSize = 16.sp,

                    fontWeight = FontWeight.Bold,

                    maxLines = 1
                )


                Spacer(
                    modifier = Modifier.size(4.dp)
                )


                Text(

                    text = song.singer,

                    fontSize = 13.sp,

                    color = MaterialTheme.colorScheme.onSurfaceVariant,

                    maxLines = 1
                )


            }


            IconButton(
                onClick = {
                    onFavoriteClick()
                }
            ) {

                Icon(

                    imageVector =
                        if (isFavorite)
                            Icons.Default.Favorite
                        else
                            Icons.Default.FavoriteBorder,

                    contentDescription =
                        if (isFavorite)
                            "取消收藏"
                        else
                            "收藏"
                )

            }


            Button(

                onClick = {
                    onPlayClick()
                }

            ) {

                Text(

                    text =
                        if (
                            isCurrentSong &&
                            isPlaying
                        )
                            "暂停"
                        else
                            "播放",

                    fontSize = 12.sp
                )

            }

        }

    }

}