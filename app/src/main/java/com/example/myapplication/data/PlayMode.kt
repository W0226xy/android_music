package com.example.myapplication.data

enum class PlayMode(val label: String) {
    SINGLE_LOOP("单曲循环"),
    LIST_LOOP("列表循环"),
    SHUFFLE("随机播放")
}

fun PlayMode.nextMode(): PlayMode {
    return when (this) {
        PlayMode.SINGLE_LOOP -> PlayMode.LIST_LOOP
        PlayMode.LIST_LOOP -> PlayMode.SHUFFLE
        PlayMode.SHUFFLE -> PlayMode.SINGLE_LOOP
    }
}