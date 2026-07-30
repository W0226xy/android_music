package com.example.myapplication.repository

import com.example.myapplication.data.Song
import com.example.myapplication.data.SongSource
import com.example.myapplication.network.RetrofitClient
import com.example.myapplication.source.LocalMusicSource
import com.example.myapplication.source.MusicSource
import com.example.myapplication.source.OnlineMusicSource
import android.content.Context
import com.example.myapplication.data.LyricContent

class MusicRepository(
    context: Context
) {

    private val musicSources: List<MusicSource> =
        listOf(
            LocalMusicSource(
                context.applicationContext
            ),
            OnlineMusicSource()
        )

    /**
     * 获取指定音源歌曲。
     */
    suspend fun getSongs(
        sourceType: SongSource
    ): List<Song> {

        return musicSources
            .firstOrNull {
                it.sourceType == sourceType
            }
            ?.getSongs()
            ?: emptyList()
    }

    /**
     * 获取所有音源歌曲。
     */
    suspend fun getAllSongs(): List<Song> {

        return musicSources.flatMap { source ->
            source.getSongs()
        }
    }

    /**
     * 刷新指定音源。
     */
    suspend fun refreshSongs(
        sourceType: SongSource
    ): List<Song> {

        return musicSources
            .firstOrNull {
                it.sourceType == sourceType
            }
            ?.refreshSongs()
            ?: emptyList()
    }



    suspend fun getLyrics(
        song: Song
    ): LyricContent {

        val musicSource =
            musicSources.firstOrNull {
                it.sourceType == song.source
            }

        return musicSource
            ?.getLyrics(song)
            ?: LyricContent.Empty
    }
}