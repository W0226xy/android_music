package com.example.myapplication.source

import android.util.Log
import com.example.myapplication.data.Song
import com.example.myapplication.data.SongSource
import com.example.myapplication.network.RetrofitClient
import com.example.myapplication.data.LyricContent
import com.example.myapplication.utils.LyricParser

class OnlineMusicSource : MusicSource {

    override val sourceType: SongSource =
        SongSource.ONLINE

    override suspend fun getSongs(): List<Song> {

        return try {

            val result =
                RetrofitClient.apiService.getJamendoSongs()

            result.mapNotNull { dto ->

                val serverId = dto.id
                val name = dto.name
                val url = dto.url

                if (
                    serverId == null ||
                    name.isNullOrBlank() ||
                    url.isNullOrBlank()
                ) {
                    return@mapNotNull null
                }

                Song(
                    serverId = serverId,

                    // 防止与本地歌曲 ID 冲突
                    id = serverId + 10000,

                    name = name,

                    singer =
                        dto.artist
                            ?.takeIf { it.isNotBlank() }
                            ?: "未知歌手",

                    album =
                        dto.album
                            ?.takeIf { it.isNotBlank() }
                            ?: "未知专辑",

                    url =
                        url.replace(
                            "localhost",
                            "10.0.2.2"
                        ),

                    coverUrl =
                        dto.coverUrl
                            ?.replace(
                                "localhost",
                                "10.0.2.2"
                            ),

                    lyricUrl =
                        dto.lyricUrl
                            ?.replace(
                                "localhost",
                                "10.0.2.2"
                            ),

                    source =
                        SongSource.ONLINE
                )
            }

        } catch (e: Exception) {

            Log.e(
                "OnlineMusicSource",
                "获取在线歌曲失败",
                e
            )

            emptyList()
        }
    }

    override suspend fun refreshSongs(): List<Song> {

        return try {

            // 让服务器重新获取 Jamendo 数据
            RetrofitClient.apiService
                .refreshJamendoSongs()

            // 再使用统一 getSongs() 获取数据
            getSongs()

        } catch (e: Exception) {

            Log.e(
                "OnlineMusicSource",
                "刷新在线歌曲失败",
                e
            )

            emptyList()
        }
    }

    override suspend fun getLyrics(
        song: Song
    ): LyricContent {

        return try {

            val serverId =
                song.serverId
                    ?: return LyricContent.Empty

            val lyricsText =
                RetrofitClient.apiService
                    .getOnlineLyrics(
                        serverId.toLong()
                    )
                    .string()

            if (lyricsText.isBlank()) {

                return LyricContent.Empty
            }

            if (
                LyricParser.hasTimestamp(
                    lyricsText
                )
            ) {

                val lines =
                    LyricParser.parseTimedLyrics(
                        lyricsText
                    )

                if (lines.isEmpty()) {
                    LyricContent.Empty
                } else {
                    LyricContent.Timed(lines)
                }

            } else {

                val lines =
                    LyricParser.parsePlainLyrics(
                        lyricsText
                    )

                if (lines.isEmpty()) {
                    LyricContent.Empty
                } else {
                    LyricContent.Plain(lines)
                }
            }

        } catch (e: Exception) {

            Log.e(
                "OnlineMusicSource",
                "获取在线歌词失败：${song.name}",
                e
            )

            LyricContent.Empty
        }
    }
}