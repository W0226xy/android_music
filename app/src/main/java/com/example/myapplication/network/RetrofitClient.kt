package com.example.myapplication.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object RetrofitClient {//网络客户端配置


    /**
     * 服务器基础URL
     *
     * 模拟器：10.0.2.2:8080（自动映射到宿主机 localhost）
     * 真机：  配合 adb reverse tcp:8080 tcp:8080 使用 127.0.0.1:8080
     */
    private const val BASE_URL = "http://127.0.0.1:8080/"


    /**
     * API服务实例
     *
     * 使用懒加载初始化Retrofit客户端并创建ApiService接口实现
     *
     * @return 配置好的ApiService实例
     */
    val apiService: ApiService by lazy {//by lazy第一次使用apiService 时才创建对象（用户不进入在线音乐不需要加载，浪费资源）
        //ApiService是自定义的接口，获取jamendo在线歌曲、刷新歌曲、获取歌词

        Retrofit.Builder()//一个 Android 网络请求框架，用接口形式封装 HTTP 请求。
            .baseUrl(BASE_URL)//服务器基础地址
            .addConverterFactory(
                GsonConverterFactory.create()//JSON 转换器，将服务器返回的 JSON 数据自动转换成 Kotlin 对象
            )
            .build()//创建 Retrofit 实例
            .create(ApiService::class.java)//创建 ApiService 接口的实现

    }

}