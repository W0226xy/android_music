package com.example.myapplication.source

import com.example.myapplication.R
import com.example.myapplication.data.Song
import com.example.myapplication.data.SongSource
import android.content.Context
import android.util.Log
import com.example.myapplication.data.LyricContent
import com.example.myapplication.utils.LyricParser


class LocalMusicSource(
    private val context: Context
) : MusicSource {

    override val sourceType: SongSource =
        SongSource.LOCAL

    override suspend fun getSongs(): List<Song> {

        return listOf(
            return listOf(



                Song(
                    id = 1,
                    name = "2002年的第一场雪",
                    singer = "刀郎",
                    album = "未知专辑",
                    audioResId = R.raw.song_2002_first_snow,
                    lyricResId = R.raw.lrc_2002_first_snow,
                    source = SongSource.LOCAL
                ),


                Song(
                    id = 2,
                    name = "Andy",
                    singer = "阿杜",
                    album = "未知专辑",
                    audioResId = R.raw.song_andy_adu,
                    lyricResId = R.raw.lrc_andy_adu,
                    source = SongSource.LOCAL
                ),


                Song(
                    id = 3,
                    name = "别说我的眼泪你无所谓",
                    singer = "东来东往",
                    album = "未知专辑",
                    audioResId = R.raw.song_tears,
                    lyricResId = R.raw.lrc_tears,
                    source = SongSource.LOCAL
                ),


                Song(
                    id = 4,
                    name = "时间你慢些走",
                    singer = "未知歌手",
                    album = "未知专辑",
                    audioResId = R.raw.song_time_slow,
                    lyricResId = R.raw.lrc_time_slow,
                    source = SongSource.LOCAL
                ),


                Song(
                    id = 5,
                    name = "爱是你我",
                    singer = "刀郎 / 云朵 / 王翰仪",
                    album = "未知专辑",
                    audioResId = R.raw.song_love_you_me,
                    lyricResId = R.raw.lrc_love_you_me,
                    source = SongSource.LOCAL
                ),


                Song(
                    id = 6,
                    name = "笨小孩",
                    singer = "刘德华",
                    album = "未知专辑",
                    audioResId = R.raw.song_benxiaohai,
                    lyricResId = R.raw.lrc_benxiaohai,
                    source = SongSource.LOCAL
                ),


                Song(
                    id = 7,
                    name = "逆浪千秋",
                    singer = "言和",
                    album = "未知专辑",
                    audioResId = R.raw.song_nilang_qianqiu,
                    lyricResId = R.raw.lrc_nilang_qianqiu,
                    source = SongSource.LOCAL
                )
            )
        )
    }

    override suspend fun getLyrics(
        song: Song
    ): LyricContent {

        Log.d(
            "LocalMusicSource",
            "开始获取本地歌词：${song.name}, lyricResId=${song.lyricResId}"
        )

        val lyricResId = song.lyricResId

        if (lyricResId == null) {

            Log.e(
                "LocalMusicSource",
                "歌曲没有 lyricResId：${song.name}"
            )

            return LyricContent.Empty
        }

        return try {

            val lines =
                LyricParser.parseLrc(
                    context,
                    lyricResId
                )

            Log.d(
                "LocalMusicSource",
                "歌词解析完成：${song.name}, 行数=${lines.size}"
            )

            if (lines.isEmpty()) {

                Log.e(
                    "LocalMusicSource",
                    "歌词解析结果为空：${song.name}"
                )

                LyricContent.Empty

            } else {

                LyricContent.Timed(lines)
            }

        } catch (e: Exception) {

            Log.e(
                "LocalMusicSource",
                "本地歌词读取失败：${song.name}",
                e
            )

            LyricContent.Empty
        }
    }
}
