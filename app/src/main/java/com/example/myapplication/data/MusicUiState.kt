package com.example.myapplication.data

data class MusicUiState(//音乐播放器当前界面状态
    val songs: List<Song> = emptyList(),//歌曲列表
    val searchText: String = "",//搜索框文本
    val currentSongId: Int = 0,//当前正在播放的歌曲 ID
    val isPlaying: Boolean = false,//当前是否正在播放
    val currentPosition: Int = 0,//当前播放进度，单位毫秒
    val duration: Int = 0,//歌曲总时长，单位也是毫秒
    val volume: Float = 1f,//音量大小
    val playMode: PlayMode = PlayMode.LIST_LOOP,//表示当前播放模式（列表循环、单曲循环、随机播放）
    val favoriteSongIds: Set<Int> = emptySet(),//喜欢音乐id集合
    val currentLyric: String = "等待歌词...",//当前正在显示的歌词
    val nextLyric: String = "",//下一句歌词
    val lyricWindow: List<String> = emptyList(),//歌词窗口
    val activeLyricIndex: Int = -1,//当前高亮歌词在 lyricWindow 里的下标
    val fullLyricLines: List<String> = emptyList(),//完整歌词列表用于滚动显示
    val playbackHistory: List<Song> = emptyList(),//播放历史记录列表
    val playbackSpeed: Float = 1f//播放速度，默认为1倍速
) {
    val currentSong: Song?//根据当前歌曲 ID，从歌曲列表中找到当前正在播放的歌曲；如果找不到，就默认返回第一首歌
        get() = songs.firstOrNull { it.id == currentSongId } ?: songs.firstOrNull()

    val filteredSongs: List<Song>//根据搜索框输入的内容，对歌曲列表进行过滤，得到搜索后的歌曲列表。
        get() = songs.filter {//ignoreCase = true忽略大小写，根据歌曲名或者歌手找
            it.name.contains(searchText, ignoreCase = true) ||
                    it.singer.contains(searchText, ignoreCase = true)
        }
}
