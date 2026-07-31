package com.example.myapplication.network

import com.example.myapplication.data.OnlineSongDto
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
//Retrofit网络通信:Android调用Spring Boot接口。
//ApiService.kt
//RetrofitClient.kt

interface ApiService {


    /**
     * 获取服务器数据库歌曲（测试用例）
     */
    @GET("songs")
    suspend fun getSongs(): List<OnlineSongDto>

    /**
     * 获取 Jamendo 在线歌曲列表
     * 从数据库获取已缓存的 Jamendo 歌曲
     *
     * @return Jamendo 歌曲列表
     */
    @GET("songs/jamendo")
    suspend fun getJamendoSongs(): List<OnlineSongDto>

    /**
     * 刷新 Jamendo 歌曲列表
     * 从 Jamendo API 获取最新歌曲并更新数据库
     */
    @GET("songs/jamendo/refresh")
    suspend fun refreshJamendoSongs(): ResponseBody

    /**
     * 获取在线歌曲的歌词
     *
     * @param songId 歌曲ID
     * @return 歌词响应体
     */
    @GET("songs/{id}/lyrics")
    suspend fun getOnlineLyrics(
        @Path("id") songId: Long
    ): ResponseBody
}