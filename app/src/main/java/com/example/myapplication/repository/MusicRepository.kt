package com.example.myapplication.repository

import com.example.myapplication.data.Song
import com.example.myapplication.network.MusicApiService

class MusicRepository {
    private val apiService = MusicApiService.create()

    suspend fun getSongs(): List<Song> {
        return apiService.getSongs()
    }
}
