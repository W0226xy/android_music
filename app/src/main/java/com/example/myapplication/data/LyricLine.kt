package com.example.myapplication.data
//Model负责描述数据:
//一首歌有什么信息
//一行歌词有什么信息
//播放模式有哪些
//歌曲列表从哪里来
//当前 UI 有哪些状态
data class LyricLine(//一行歌词 + 这行歌词对应的播放时间
    val timeMs: Int,
    val text: String
)