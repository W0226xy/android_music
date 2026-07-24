package com.example.myapplication.repository

import android.util.Log
import com.example.myapplication.R
import com.example.myapplication.data.Song
import com.example.myapplication.data.SongSource
import com.example.myapplication.network.RetrofitClient


class MusicRepository {


    /**
     * 获取本地歌曲
     */
    fun getLocalSongs(): List<Song> {

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
    }


    /**
     * 获取服务器歌曲
     */
    suspend fun getOnlineSongs(): List<Song> {

        return try {


            val result = RetrofitClient.apiService.getSongs()


            Log.d(
                "Repository",
                "server result=$result"
            )


            result.mapNotNull { dto ->

                Log.d(
                    "Repository",
                    "coverUrl=${dto.coverUrl}"
                )


                val id = dto.id
                val name = dto.name
                val url = dto.url


                // 过滤无效数据
                if (id == null ||
                    name.isNullOrBlank() ||
                    url.isNullOrBlank()
                ) {

                    Log.w(
                        "Repository",
                        "忽略无效在线歌曲：$dto"
                    )

                    return@mapNotNull null
                }


                Song(

                    // 在线歌曲使用独立 ID，避免和本地歌曲冲突
                    id = id + 10000,


                    name = name,


                    singer = dto.artist
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "未知歌手",


                    album = dto.album
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "未知专辑",


                    url = url.replace(
                        "localhost",
                        "10.0.2.2"
                    ),


                    coverUrl = dto.coverUrl
                        ?.replace(
                            "localhost",
                            "10.0.2.2"
                        ),


                    lyricUrl = dto.lyricUrl
                        ?.replace(
                            "localhost",
                            "10.0.2.2"
                        ),


                    source = SongSource.ONLINE
                )

            }


        } catch (e: Exception) {


            Log.e(
                "Repository",
                "get online songs failed",
                e
            )


            emptyList()

        }

    }

}