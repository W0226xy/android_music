package com.example.myapplication.network

import com.example.myapplication.data.Song
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET

interface MusicApiService {
    @GET("api/songs")
    suspend fun getSongs(): List<Song>

    companion object {
        private const val BASE_URL = "http://10.0.2.2:8080/"

        private val json = Json {
            ignoreUnknownKeys = true
        }

        fun create(): MusicApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(MusicApiService::class.java)
        }
    }
}