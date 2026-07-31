package com.example.myapplication.playback
import android.util.Log
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
//把自己定义的Song转化为MediaItem
// MediaItem 中包含两类信息：
//1.播放信息：
//歌曲音频地址 URI
//2.展示信息：
//歌曲名、歌手、专辑、封面
    context: Context
): MediaItem? {

    val mediaUri = when (source) {//根据歌曲来源生成播放地址
        SongSource.LOCAL -> {
            val resId = audioResId//audioResId类似R.raw.song_2002_first_snow
                ?.takeIf { it != 0 }
                ?: return null

            Uri.parse(//将资源id转化为uri
                "android.resource://${context.packageName}/$resId"
            )
            //eg:本地歌曲 2002年的第一场雪，resId=2131296263，生成的Uri=android.resource://com.example.myapplication/2131296263

        }

        SongSource.ONLINE -> {
            val onlineUrl = url
                ?.takeIf { it.isNotBlank() }
                ?: return null

            Uri.parse(onlineUrl)
        }
    }

    val metadataBuilder = MediaMetadata.Builder()
        .setTitle(name)//这里toMediaItemOrNull() 是一个 Song 的扩展函数，name是相当于this.name，this是Song
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