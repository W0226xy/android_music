package com.example.myapplication.network

import com.example.myapplication.data.Song
import retrofit2.http.GET
import com.example.myapplication.data.OnlineSongDto

interface ApiService {

    @GET("songs")
    suspend fun getSongs(): List<OnlineSongDto>

}