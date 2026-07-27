package com.example.myapplication.network

import com.example.myapplication.data.OnlineSongDto
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path


interface ApiService {


    /**
     * 获取服务器数据库歌曲
     */
    @GET("songs")
    suspend fun getSongs(): List<OnlineSongDto>



    /**
     * 获取 Jamendo 在线歌曲
     */
    @GET("songs/jamendo")
    suspend fun getJamendoSongs(): List<OnlineSongDto>

    @GET("songs/jamendo/refresh")
    suspend fun refreshJamendoSongs(): ResponseBody

    @GET("songs/{id}/lyrics")
    suspend fun getOnlineLyrics(
        @Path("id") songId: Long
    ): ResponseBody
}