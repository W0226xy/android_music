package com.example.myapplication.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.example.myapplication.data.Song
import com.example.myapplication.data.SongSource

/**
 * 将项目中的 Song 转换成 Media3 使用的 MediaItem。
 *
 * 转换失败时返回 null，例如：
 * 1. 本地歌曲没有 audioResId；
 * 2. 在线歌曲没有 url。
 */
fun Song.toMediaItemOrNull(
    context: Context
): MediaItem? {

    val mediaUri = when (source) {
        SongSource.LOCAL -> {
            val resId = audioResId
                ?.takeIf { it != 0 }
                ?: return null

            Uri.parse(
                "android.resource://${context.packageName}/$resId"
            )
        }

        SongSource.ONLINE -> {
            val onlineUrl = url
                ?.takeIf { it.isNotBlank() }
                ?: return null

            Uri.parse(onlineUrl)
        }
    }

    val metadataBuilder = MediaMetadata.Builder()
        .setTitle(name)
        .setArtist(singer)
        .setAlbumTitle(album)
        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)

    coverUrl
        ?.takeIf { it.isNotBlank() }
        ?.let { metadataBuilder.setArtworkUri(Uri.parse(it)) }

    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(mediaUri)
        .setMediaMetadata(metadataBuilder.build())
        .build()
}