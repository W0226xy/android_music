package com.example.myapplication.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object RetrofitClient {


    /**
     * Spring Boot服务器地址
     *
     * Android模拟器访问电脑localhost:
     * 10.0.2.2 = 电脑localhost
     */
    private const val BASE_URL = "http://10.0.2.2:8080/"


    val apiService: ApiService by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(ApiService::class.java)

    }

}