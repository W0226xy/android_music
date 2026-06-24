package com.example.myapplication.data

data class MusicUiState(
    val songs: List<Song> = emptyList(),
    val searchText: String = "",
    val currentSongId: Int = 0,
    val isPlaying: Boolean = false,
    val currentPosition: Int = 0,
    val duration: Int = 0,
    val volume: Float = 1f,
    val playMode: PlayMode = PlayMode.LIST_LOOP,
    val favoriteSongIds: Set<Int> = emptySet(),
    val currentLyric: String = "等待歌词...",
    val nextLyric: String = "",
    val lyricWindow: List<String> = emptyList(),
    val activeLyricIndex: Int = -1
) {
    val currentSong: Song?
        get() = songs.firstOrNull { it.id == currentSongId } ?: songs.firstOrNull()

    val filteredSongs: List<Song>
        get() = songs.filter {
            it.name.contains(searchText, ignoreCase = true) ||
                    it.singer.contains(searchText, ignoreCase = true)
        }
}