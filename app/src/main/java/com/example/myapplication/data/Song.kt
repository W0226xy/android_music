package com.example.myapplication.data

import androidx.annotation.RawRes
import com.google.gson.annotations.SerializedName


data class Song(

    val id: Int,

    val name: String,


    // 对应服务器 artist 字段
    @SerializedName("artist")
    val singer: String,


    val album: String = "未知专辑",


    // ========== 本地资源 ==========

    @RawRes
    val audioResId: Int? = null,


    @RawRes
    val lyricResId: Int? = null,


    // ========== 在线资源 ==========

    val url: String? = null,


    @SerializedName("cover_url")
    val coverUrl: String? = null,


    @SerializedName("lyric_url")
    val lyricUrl: String? = null,


    // 歌曲来源
    val source: SongSource = SongSource.LOCAL

)