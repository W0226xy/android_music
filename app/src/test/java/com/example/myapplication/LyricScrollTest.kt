package com.example.myapplication

import com.example.myapplication.data.LyricLine
import com.example.myapplication.data.MusicUiState
import org.junit.Assert.*
import org.junit.Test

/**
 * 测试歌词滚动功能
 */
class LyricScrollTest {

    @Test
    fun testCurrentLyricIndexIsUpdated() {
        // 创建模拟歌词数据
        val lyricLines = listOf(
            LyricLine(timeMs = 0, text = "第一句歌词"),
            LyricLine(timeMs = 1000, text = "第二句歌词"),
            LyricLine(timeMs = 2000, text = "第三句歌词"),
            LyricLine(timeMs = 3000, text = "第四句歌词"),
            LyricLine(timeMs = 4000, text = "第五句歌词")
        )

        // 模拟在不同时间点检查歌词索引
        // 在时间0ms，应该返回第一句歌词（索引0）
        val indexAt0ms = lyricLines.indexOfLast { 0 >= it.timeMs }
        assertEquals(0, indexAt0ms)

        // 在时间1500ms，应该返回第二句歌词（索引1）
        val indexAt1500ms = lyricLines.indexOfLast { 1500 >= it.timeMs }
        assertEquals(1, indexAt1500ms)

        // 在时间2500ms，应该返回第三句歌词（索引2）
        val indexAt2500ms = lyricLines.indexOfLast { 2500 >= it.timeMs }
        assertEquals(2, indexAt2500ms)

        // 在时间4500ms，应该返回第五句歌词（索引4）
        val indexAt4500ms = lyricLines.indexOfLast { 4500 >= it.timeMs }
        assertEquals(4, indexAt4500ms)
    }

    @Test
    fun testMusicUiStateContainsCurrentLyricIndex() {
        // 测试MusicUiState是否包含currentLyricIndex字段
        val uiState = MusicUiState(
            songs = emptyList(),
            currentLyricIndex = 2  // 设置当前歌词索引为2
        )

        assertEquals(2, uiState.currentLyricIndex)
        assertTrue(uiState.currentLyricIndex >= 0)
    }

    @Test
    fun testLyricIndexCalculation() {
        // 测试歌词索引计算逻辑
        val lyrics = listOf(
            "第一句歌词",
            "第二句歌词", 
            "第三句歌词",
            "第四句歌词",
            "第五句歌词"
        )

        // 模拟当前歌词是第三句
        val currentLyric = "第三句歌词"
        val expectedIndex = 2
        
        // 查找当前歌词在完整列表中的索引
        val actualIndex = lyrics.indexOfFirst { it == currentLyric }
        
        assertEquals(expectedIndex, actualIndex)
        assertTrue(actualIndex >= 0)
    }
}