package com.example.myapplication.repository

import android.util.Log
import com.example.myapplication.R
import com.example.myapplication.data.Song
import com.example.myapplication.data.SongSource
import com.example.myapplication.network.RetrofitClient


class MusicRepository {

    /**
     * 获取本地歌曲列表
     * 返回硬编码在应用内的本地歌曲数据
     *
     * @return 本地歌曲列表
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
    suspend fun getOnlineSongs(): List<Song> {//suspend 说明这是一个挂起函数。因为网络请求耗时,不能因此阻塞主线程。
    //suspend一般表示这个函数应该在协程中被调用
        return try {//这里try catch是防止网络异常导致app崩溃


            val result = RetrofitClient.apiService.getJamendoSongs()//调用服务器接口获取Jamendo歌曲
            //JamendoService会返回json格式数据，Retrofit + Gson会转化为类似list<song>的对象

            Log.d(
                "Repository",
                "server result=$result"
            )

            //遍历服务器返回的每一首歌曲
            result.mapNotNull { dto ->

                Log.d(
                    "Repository",
                    "coverUrl=${dto.coverUrl}"
                )

                val id = dto.id//歌曲id
                val name = dto.name//歌曲名称
                val url = dto.url//播放地址


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
                    // 服务器真实ID
                    serverId = id,
                    // 应用内部ID（独立ID，避免和本地歌曲冲突）
                    id = id + 10000,
                    //左边的id是客户端的Song id
                    //右边id是从服务器获取到的歌曲id



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

    /**
     * 获取在线歌曲的歌词
     * 通过歌曲ID从服务器获取对应歌词
     *
     * @param songId 歌曲ID
     * @return 歌词字符串
     */
    suspend fun getOnlineLyrics(songId: Long): String {//获取在线音乐歌曲歌词
        return RetrofitClient.apiService
            .getOnlineLyrics(songId)
            .string()
    }

    /**
     * 刷新在线歌曲列表
     * 先请求服务器刷新Jamendo数据库，然后重新获取最新歌曲
     *
     * @return 刷新后的在线歌曲列表
     */
    suspend fun refreshOnlineSongs(): List<Song> {

        return try {

            Log.d(
                "Repository",
                "开始刷新 Jamendo 在线歌曲"
            )

            // 先让服务器刷新数据库
            RetrofitClient.apiService.refreshJamendoSongs()

            Log.d(
                "Repository",
                "服务器刷新完成，重新获取在线歌曲"
            )

            // 再重新读取最新歌曲列表
            getOnlineSongs()

        } catch (e: Exception) {

            Log.e(
                "Repository",
                "刷新在线歌曲失败：${e.message}",
                e
            )

            emptyList()
        }
    }

}