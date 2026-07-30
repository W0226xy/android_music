package com.example.myapplication.source

import com.example.myapplication.data.LyricContent
import com.example.myapplication.data.Song
import com.example.myapplication.data.SongSource

/**
 * 所有音乐数据源的公共接口。
 *
 * 不管歌曲来自本地资源还是网络，
 * 对上层统一暴露相同的方法。
 */
interface MusicSource {

    /**
     * 当前音源类型。
     */
    val sourceType: SongSource

    /**
     * 获取当前音源中的歌曲列表。
     */
    suspend fun getSongs(): List<Song>


    /**
     * 获取指定歌曲歌词
     */
    suspend fun getLyrics(
        song: Song
    ): LyricContent

    /**
     * 刷新当前音源。
     *
     * 默认重新读取歌曲。
     * 在线音源可以覆盖该方法执行真正的网络刷新。
     */
    suspend fun refreshSongs(): List<Song> {
        return getSongs()
    }
}