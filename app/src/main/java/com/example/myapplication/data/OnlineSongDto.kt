package com.example.myapplication.data

import com.google.gson.annotations.SerializedName

data class OnlineSongDto(
    val id: Int?,
    val name: String?,

    @SerializedName("artist")
    val artist: String?,

    val album: String?,
    val url: String?,
    val coverUrl: String?,
    val lyricUrl: String?
)