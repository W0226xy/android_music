package com.example.myapplication.utils

import android.content.Context
import androidx.annotation.RawRes
import com.example.myapplication.data.LyricLine
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

object LyricParser {

    fun parseLrc(context: Context, @RawRes lyricResId: Int): List<LyricLine> {
        val lrcText = readRawText(context, lyricResId)

        val timeRegex = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")
        val lyricLines = mutableListOf<LyricLine>()

        lrcText.lines().forEach { line ->
            val matches = timeRegex.findAll(line).toList()
            val text = line.replace(timeRegex, "").trim()

            if (matches.isNotEmpty() && text.isNotBlank()) {
                matches.forEach { match ->
                    val minute = match.groupValues[1].toInt()
                    val second = match.groupValues[2].toInt()

                    val fractionText = match.groupValues.getOrNull(3).orEmpty()
                    val millis = when (fractionText.length) {
                        0 -> 0
                        1 -> fractionText.toInt() * 100
                        2 -> fractionText.toInt() * 10
                        else -> fractionText.take(3).toInt()
                    }

                    val timeMs = minute * 60 * 1000 + second * 1000 + millis

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