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

<<<<<<< HEAD
object LyricParser {//读取歌词文件和时间戳

    fun parseLrc(context: Context, @RawRes lyricResId: Int): List<LyricLine> {
        val lrcText = readRawText(context, lyricResId)

        val timeRegex = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")
        val lyricLines = mutableListOf<LyricLine>()

        lrcText.lines().forEach { line ->
            val matches = timeRegex.findAll(line).toList()
            val text = line.replace(timeRegex, "").trim()
=======

object LyricParser {


    /**
     * 解析本地 raw 目录中的歌词
     */
    fun parseLrc(
        context: Context,
        @RawRes lyricResId: Int
    ): List<LyricLine> {
        val lrcText =
            readRawText(
                context,
                lyricResId
            )
        return parseLrcText(lrcText)

    }



    /**
     * 解析网络歌词
     *
     * 例如:
     * http://10.0.2.2:8080/lyric/test.lrc
     */
    suspend fun parseNetworkLrc(
        lyricUrl: String?
    ): List<LyricLine> {
        if (lyricUrl.isNullOrBlank()) {
            return emptyList()
        }

        return try {
            val lrcText =
                withContext(Dispatchers.IO) {
                    URL(lyricUrl)
                        .readText(Charsets.UTF_8)
                }
            parseLrcText(lrcText)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()

        }

    }



    /**
     * 通用歌词解析
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


        val standardTimeRegex =
            Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")


        val specialTimeRegex =
            Regex("""\[(\d+):(\d+)\.(\d+)(?:\.(\d+))?]""")


        val lyricLines =
            mutableListOf<LyricLine>()



        lrcText.lines().forEach { line ->


            // 优先匹配特殊格式
            var matches =
                specialTimeRegex.findAll(line)
                    .toList()


            var text =
                line.replace(
                    specialTimeRegex,
                    ""
                ).trim()


            var useSpecialFormat = true



            // 没有特殊格式，匹配标准格式
            if (matches.isEmpty()) {


                matches =
                    standardTimeRegex.findAll(line)
                        .toList()


                text =
                    line.replace(
                        standardTimeRegex,
                        ""
                    ).trim()


                useSpecialFormat = false

            }
>>>>>>> 1f3ec53 (增加在线音乐服务器功能，目前服务器测试歌曲可被正确读取到客户端)



            if (matches.isNotEmpty() && text.isNotBlank()) {


                matches.forEach { match ->
<<<<<<< HEAD
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
=======


                    val timeMs = if (useSpecialFormat) {


                        val minute =
                            match.groupValues[1]
                                .toInt()


                        val second =
                            match.groupValues[2]
                                .toInt()


                        val millisPart =
                            match.groupValues[3]



                        val millis =
                            when {


                                millisPart.isEmpty()
                                    -> 0


                                millisPart.length == 1
                                    -> millisPart.toInt() * 100


                                millisPart.length == 2
                                    -> millisPart.toInt() * 10


                                else
                                    -> millisPart.take(3).toInt()

                            }



                        minute * 60 * 1000 +
                                second * 1000 +
                                millis



                    } else {


                        val minute =
                            match.groupValues[1]
                                .toInt()


                        val second =
                            match.groupValues[2]
                                .toInt()



                        val fractionText =
                            match.groupValues
                                .getOrNull(3)
                                .orEmpty()



                        val millis =
                            when(fractionText.length) {


                                0 -> 0


                                1 ->
                                    fractionText.toInt() * 100


                                2 ->
                                    fractionText.toInt() * 10


                                else ->
                                    fractionText.take(3).toInt()

                            }



                        minute * 60 * 1000 +
                                second * 1000 +
                                millis

                    }


>>>>>>> 1f3ec53 (增加在线音乐服务器功能，目前服务器测试歌曲可被正确读取到客户端)

                    lyricLines.add(

                        LyricLine(

                            timeMs = timeMs,

                            text = text

                        )

                    )


                }

            }


        }



        return lyricLines.sortedBy {

            it.timeMs

        }

    }




    /**
     * 读取 res/raw 中的歌词文件
     *
     * 支持 UTF-8 / GBK
     */
    private fun readRawText(
        context: Context,
        @RawRes resId: Int
    ): String {


        val bytes =
            context.resources
                .openRawResource(resId)
                .use {

                    it.readBytes()

                }



        val charsets =
            listOf(

                Charsets.UTF_8,

                Charset.forName("GBK")

            )



        for (charset in charsets) {


            try {


                val decoder =
                    charset.newDecoder()
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


            }


        }



        return String(
            bytes,
            Charsets.UTF_8
        )

    }


}



/**
 * 毫秒转换成 mm:ss
 */
fun formatTime(
    ms: Int
): String {


    val totalSeconds =
        ms / 1000


    val minute =
        totalSeconds / 60


    val second =
        totalSeconds % 60



    return "%02d:%02d".format(
        minute,
        second
    )

}