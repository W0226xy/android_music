package com.example.myapplication.data

import androidx.annotation.RawRes

data class Song(
    val id: Int,
    val name: String,
    val singer: String,
    val album: String,
    @RawRes val audioResId: Int,
    @RawRes val lyricResId: Int
)