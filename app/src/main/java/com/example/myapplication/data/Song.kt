package com.example.myapplication.data

import kotlinx.serialization.Serializable

@Serializable
data class Song(
    val id: Int = 1000,
    val name: String = "2002年的第一场雪",
    val singer: String = "刀郎",
    val album: String = "未知专辑",
    val audioUrl: String = "", // 音频网络地址
    val lyricUrl: String = "" // 歌词网络地址
)
//
//id = 1,
//name = "2002年的第一场雪",
//singer = "刀郎",
//album = "未知专辑",
//audioResId = R.raw.song_2002_first_snow,
//lyricResId = R.raw.lrc_2002_first_snow