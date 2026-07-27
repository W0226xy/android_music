package com.example.myapplication.network

import com.example.myapplication.data.OnlineSongDto
import retrofit2.http.GET


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

}