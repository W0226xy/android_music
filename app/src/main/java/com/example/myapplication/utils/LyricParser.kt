package com.example.myapplication.utils

import android.content.Context
import androidx.annotation.RawRes
import com.example.myapplication.data.LyricLine
import okhttp3.OkHttpClient
import okhttp3.Request
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.io.IOException

object LyricParser {//读取歌词文件和时间戳
//object只有一个单例
    // 从资源ID加载歌词
    fun parseLrc(context: Context, @RawRes lyricResId: Int): List<LyricLine> {
        val lrcText = readRawText(context, lyricResId)
        return parseLrcText(lrcText)
    }
    
    // 从网络URL加载歌词
    @Throws(IOException::class)
    fun parseLrcFromUrl(url: String): List<LyricLine> {
        val lrcText = downloadLrcFromUrl(url)
        return parseLrcText(lrcText)
    }
    
    // 解析LRC文本内容
    private fun parseLrcText(lrcText: String): List<LyricLine> {

        // 支持多种LRC时间戳格式：
        // 1. 标准格式: [mm:ss.xxx] 或 [mm:ss:xxx]
        // 2. 特殊格式: [m:s.ms.xx] (如 [0:9.970.00], [2:15.520.00])
        //    实际含义: [分:秒.毫秒.xx] (例如: [0:9.970.00] = 0分 + 9秒 + 970毫秒 = 9970毫秒)
        val standardTimeRegex = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")
        val specialTimeRegex = Regex("""\[(\d+):(\d+)\.(\d+)(?:\.(\d+))?]""")

        val lyricLines = mutableListOf<LyricLine>()

        lrcText.lines().forEach { line ->
            // 先尝试特殊格式
            var matches = specialTimeRegex.findAll(line).toList()
            var text = line.replace(specialTimeRegex, "").trim()
            var useSpecialFormat = true

            // 如果特殊格式没有匹配，尝试标准格式
            if (matches.isEmpty()) {
                matches = standardTimeRegex.findAll(line).toList()
                text = line.replace(standardTimeRegex, "").trim()
                useSpecialFormat = false
            }

            if (matches.isNotEmpty() && text.isNotBlank()) {
                matches.forEach { match ->
                    val timeMs = if (useSpecialFormat) {
                        // 特殊格式: [m:s.ms.xx] (分:秒.毫秒.xx)
                        // 例如: [0:9.970.00] = 0分 + 9秒 + 970毫秒 = 9970毫秒
                        //       [2:15.520.00] = 2分 + 15秒 + 520毫秒 = 135520毫秒
                        val minute = match.groupValues[1].toInt()
                        val second = match.groupValues[2].toInt()
                        val millisPart = match.groupValues[3]  // 毫秒部分，如 970 表示 970毫秒
                        
                        // 计算毫秒：根据长度决定
                        val millis = when {
                            millisPart.isEmpty() -> 0
                            millisPart.length == 1 -> millisPart.toInt() * 100
                            millisPart.length == 2 -> millisPart.toInt() * 10
                            millisPart.length >= 3 -> millisPart.take(3).toInt()
                            else -> 0
                        }
                        
                        minute * 60 * 1000 + second * 1000 + millis
                    } else {
                        // 标准格式: [mm:ss.xxx] 或 [mm:ss:xxx]
                        val minute = match.groupValues[1].toInt()
                        val second = match.groupValues[2].toInt()

                        val fractionText = match.groupValues.getOrNull(3).orEmpty()
                        val millis = when (fractionText.length) {
                            0 -> 0
                            1 -> fractionText.toInt() * 100
                            2 -> fractionText.toInt() * 10
                            else -> fractionText.take(3).toInt()
                        }

                        minute * 60 * 1000 + second * 1000 + millis
                    }

                    lyricLines.add(
                        LyricLine(
                            timeMs = timeMs,
                            text = text
                        )
                    )
                }
            }
        }

        return lyricLines.sortedBy { it.timeMs }
    }
    
    // 从网络下载LRC文件
    private fun downloadLrcFromUrl(url: String): String {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to download lyrics: $response")
            return response.body?.string() ?: ""
        }
    }

    private fun readRawText(context: Context, @RawRes resId: Int): String {
        val bytes = context.resources.openRawResource(resId).use {
            it.readBytes()
        }

        val charsets = listOf(
            Charsets.UTF_8,
            Charset.forName("GBK")
        )

        for (charset in charsets) {
            try {
                val decoder = charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)

                return decoder.decode(ByteBuffer.wrap(bytes)).toString()
            } catch (_: Exception) {
            }
        }

        return String(bytes, Charsets.UTF_8)
    }
}

fun formatTime(ms: Int): String {
    val totalSeconds = ms / 1000
    val minute = totalSeconds / 60
    val second = totalSeconds % 60
    return "%02d:%02d".format(minute, second)
}