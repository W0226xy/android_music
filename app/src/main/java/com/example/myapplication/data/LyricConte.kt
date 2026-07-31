
package com.example.myapplication.data

/**
 * 统一的歌词数据类型。
 *
 * Timed：带时间戳歌词，可以跟随播放进度滚动
 * Plain：普通文本歌词，只进行全文显示
 * Empty：没有歌词
 */
sealed interface LyricContent {

    data class Timed(
        val lines: List<LyricLine>
    ) : LyricContent

    data class Plain(
        val lines: List<String>
    ) : LyricContent

    object Empty : LyricContent
}