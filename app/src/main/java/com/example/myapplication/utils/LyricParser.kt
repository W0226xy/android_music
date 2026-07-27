package com.example.myapplication.utils

import android.content.Context
import androidx.annotation.RawRes
import com.example.myapplication.data.LyricLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

object LyricParser {

    /**
     * 标准时间戳格式：
     *
     * [01:23]
     * [01:23.4]
     * [01:23.45]
     * [01:23.456]
     * [01:23:456]
     */
    private val standardTimeRegex =
        Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")

    /**
     * 特殊时间戳格式：
     *
     * [1:2.345]
     * [1:2.34.56]
     */
    private val specialTimeRegex =
        Regex("""\[(\d+):(\d+)\.(\d+)(?:\.(\d+))?]""")

    /**
     * 解析本地 raw 目录中的 LRC 歌词。
     */
    fun parseLrc(
        context: Context,
        @RawRes lyricResId: Int
    ): List<LyricLine> {
        val lrcText = readRawText(
            context = context,
            resId = lyricResId
        )

        return parseLrcText(lrcText)
    }

    /**
     * 解析网络 LRC 歌词。
     *
     * 适合具有时间戳的歌词文件，例如：
     *
     * http://10.0.2.2:8080/lyric/test.lrc
     */
    suspend fun parseNetworkLrc(
        lyricUrl: String?
    ): List<LyricLine> {
        if (lyricUrl.isNullOrBlank()) {
            return emptyList()
        }

        return try {
            val lrcText = loadNetworkText(lyricUrl)

            parseLrcText(lrcText)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 从网络获取原始歌词文本。
     *
     * 既可以用于有时间戳歌词，
     * 也可以用于 Jamendo 这种无时间戳歌词。
     */
    suspend fun loadNetworkLyricsText(
        lyricUrl: String?
    ): String {
        if (lyricUrl.isNullOrBlank()) {
            return ""
        }

        return try {
            loadNetworkText(lyricUrl)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * 判断歌词文本中是否存在时间戳。
     *
     * 返回 true：
     * 按照 LRC 歌词处理，可以跟随播放进度。
     *
     * 返回 false：
     * 按照普通歌词处理，只进行手动滚动显示。
     */
    fun hasTimestamp(
        lyricsText: String
    ): Boolean {
        if (lyricsText.isBlank()) {
            return false
        }

        return standardTimeRegex.containsMatchIn(lyricsText) ||
                specialTimeRegex.containsMatchIn(lyricsText)
    }

    /**
     * 解析已经获取到的 LRC 歌词文本。
     *
     * 适用于服务器接口直接返回歌词字符串的情况。
     */
    fun parseTimedLyrics(
        lyricsText: String
    ): List<LyricLine> {
        if (lyricsText.isBlank()) {
            return emptyList()
        }

        return parseLrcText(lyricsText)
    }

    /**
     * 解析无时间戳的普通歌词。
     *
     * 例如 Jamendo 返回：
     *
     * There's not a single day that passes,
     * without you on my mind.
     *
     * 普通歌词不会生成虚假的时间戳，
     * 返回全部歌词行，由用户自己上下滚动查看。
     */
    fun parsePlainLyrics(
        lyricsText: String
    ): List<String> {
        if (lyricsText.isBlank()) {
            return emptyList()
        }

        return lyricsText
            .normalizeLineBreaks()
            .lineSequence()
            .map { line ->
                line.trim()
            }
            .filter { line ->
                line.isNotEmpty()
            }
            .toList()
    }

    /**
     * 通用 LRC 歌词解析。
     *
     * 支持：
     *
     * 1. [mm:ss.xxx]
     * 2. [mm:ss:xxx]
     * 3. [m:s.xxx.xx]
     */
    private fun parseLrcText(
        lrcText: String
    ): List<LyricLine> {
        if (lrcText.isBlank()) {
            return emptyList()
        }

        val lyricLines = mutableListOf<LyricLine>()

        lrcText
            .normalizeLineBreaks()
            .lines()
            .forEach { line ->

                // 优先匹配特殊格式
                var matches = specialTimeRegex
                    .findAll(line)
                    .toList()

                var text = line
                    .replace(
                        specialTimeRegex,
                        ""
                    )
                    .trim()

                var useSpecialFormat = true

                // 没有特殊格式时，再匹配标准格式
                if (matches.isEmpty()) {
                    matches = standardTimeRegex
                        .findAll(line)
                        .toList()

                    text = line
                        .replace(
                            standardTimeRegex,
                            ""
                        )
                        .trim()

                    useSpecialFormat = false
                }

                if (matches.isEmpty() || text.isBlank()) {
                    return@forEach
                }

                matches.forEach { match ->
                    val timeMs = if (useSpecialFormat) {
                        parseSpecialTimestamp(match)
                    } else {
                        parseStandardTimestamp(match)
                    }

                    lyricLines.add(
                        LyricLine(
                            timeMs = timeMs,
                            text = text
                        )
                    )
                }
            }

        return lyricLines
            .sortedBy { lyricLine ->
                lyricLine.timeMs
            }
    }

    /**
     * 解析特殊时间戳。
     *
     * 例如：
     *
     * [1:2.345]
     * [1:2.34.56]
     */
    private fun parseSpecialTimestamp(
        match: MatchResult
    ): Int {
        val minute = match.groupValues
            .getOrNull(1)
            .orEmpty()
            .toIntOrNull()
            ?: 0

        val second = match.groupValues
            .getOrNull(2)
            .orEmpty()
            .toIntOrNull()
            ?: 0

        val millisPart = match.groupValues
            .getOrNull(3)
            .orEmpty()

        val millis = parseMillisPart(millisPart)

        return minute * 60 * 1000 +
                second * 1000 +
                millis
    }

    /**
     * 解析标准时间戳。
     *
     * 例如：
     *
     * [01:23]
     * [01:23.45]
     * [01:23.456]
     * [01:23:456]
     */
    private fun parseStandardTimestamp(
        match: MatchResult
    ): Int {
        val minute = match.groupValues
            .getOrNull(1)
            .orEmpty()
            .toIntOrNull()
            ?: 0

        val second = match.groupValues
            .getOrNull(2)
            .orEmpty()
            .toIntOrNull()
            ?: 0

        val fractionText = match.groupValues
            .getOrNull(3)
            .orEmpty()

        val millis = parseMillisPart(fractionText)

        return minute * 60 * 1000 +
                second * 1000 +
                millis
    }

    /**
     * 将歌词时间戳的小数部分转换成毫秒。
     *
     * 例如：
     *
     * 1   -> 100 ms
     * 12  -> 120 ms
     * 123 -> 123 ms
     */
    private fun parseMillisPart(
        millisPart: String
    ): Int {
        return when (millisPart.length) {
            0 -> 0

            1 -> {
                millisPart.toIntOrNull()
                    ?.times(100)
                    ?: 0
            }

            2 -> {
                millisPart.toIntOrNull()
                    ?.times(10)
                    ?: 0
            }

            else -> {
                millisPart
                    .take(3)
                    .toIntOrNull()
                    ?: 0
            }
        }
    }

    /**
     * 获取网络歌词文本。
     */
    private suspend fun loadNetworkText(
        lyricUrl: String
    ): String {
        return withContext(Dispatchers.IO) {
            URL(lyricUrl)
                .readText(Charsets.UTF_8)
        }
    }

    /**
     * 读取 res/raw 中的歌词文件。
     *
     * 支持 UTF-8 和 GBK。
     */
    private fun readRawText(
        context: Context,
        @RawRes resId: Int
    ): String {
        val bytes = context.resources
            .openRawResource(resId)
            .use { inputStream ->
                inputStream.readBytes()
            }

        val charsets = listOf(
            Charsets.UTF_8,
            Charset.forName("GBK")
        )

        for (charset in charsets) {
            try {
                val decoder = charset
                    .newDecoder()
                    .onMalformedInput(
                        CodingErrorAction.REPORT
                    )
                    .onUnmappableCharacter(
                        CodingErrorAction.REPORT
                    )

                return decoder
                    .decode(
                        ByteBuffer.wrap(bytes)
                    )
                    .toString()
            } catch (_: Exception) {
                // 当前编码解析失败，继续尝试下一个编码
            }
        }

        return String(
            bytes,
            Charsets.UTF_8
        )
    }

    /**
     * 统一处理 Windows、Linux 和旧 Mac 换行符。
     */
    private fun String.normalizeLineBreaks(): String {
        return replace("\r\n", "\n")
            .replace("\r", "\n")
    }
}

/**
 * 毫秒转换为 mm:ss。
 */
fun formatTime(
    ms: Int
): String {
    val safeMs = ms.coerceAtLeast(0)

    val totalSeconds = safeMs / 1000
    val minute = totalSeconds / 60
    val second = totalSeconds % 60

    return "%02d:%02d".format(
        minute,
        second
    )
}