package com.example.myapplication.data

import androidx.annotation.RawRes

data class Song(
    val id: Int,
    val name: String,
    val singer: String,
    val album: String,
    @RawRes val audioResId: Int,// 音频资源 ID，对应 res/raw 目录下的音频文件，例如 R.raw.song
    @RawRes val lyricResId: Int// 歌词资源 ID，对应 res/raw 目录下的歌词文件，例如 R.raw.song_lrc
)