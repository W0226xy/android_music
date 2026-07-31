package com.example.music.server

import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.routing.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable

@Serializable
data class Song(
    val id: Int,
    val name: String,
    val singer: String,
    val album: String,
    val audioUrl: String,
    val lyricUrl: String,
)

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    install(CORS) {
        anyHost()
        allowHeaders { true }
        HttpMethod.DefaultMethods.forEach { allowMethod(it) }
        allowCredentials = true
    }
    
    install(ContentNegotiation) {
        json()
    }
    
    val songs = listOf(
        Song(
            id = 1,
            name = "2002年的第一场雪",
            singer = "刀郎",
            album = "未知专辑",
            audioUrl = "http://10.0.2.2:8080/static/song_2002_first_snow.mp3",
            lyricUrl = "http://10.0.2.2:8080/static/lrc_2002_first_snow.lrc"
        ),
        Song(
            id = 2,
            name = "Andy",
            singer = "阿杜",
            album = "未知专辑",
            audioUrl = "http://10.0.2.2:8080/static/song_andy_adu.mp3",
            lyricUrl = "http://10.0.2.2:8080/static/lrc_andy_adu.lrc"
        ),
        Song(
            id = 3,
            name = "别说我的眼泪你无所谓",
            singer = "东来东往",
            album = "未知专辑",
            audioUrl = "http://10.0.2.2:8080/static/song_tears.mp3",
            lyricUrl = "http://10.0.2.2:8080/static/lrc_tears.lrc"
        ),
        Song(
            id = 4,
            name = "时间你慢些走",
            singer = "未知歌手",
            album = "未知专辑",
            audioUrl = "http://10.0.2.2:8080/static/song_time_slow.mp3",
            lyricUrl = "http://10.0.2.2:8080/static/lrc_time_slow.lrc"
        ),
        Song(
            id = 5,
            name = "爱是你我",
            singer = "刀郎 / 云朵 / 王翰仪",
            album = "未知专辑",
            audioUrl = "http://10.0.2.2:8080/static/song_love_you_me.mp3",
            lyricUrl = "http://10.0.2.2:8080/static/lrc_love_you_me.lrc"
        ),
        Song(
            id = 6,
            name = "笨小孩",
            singer = "刘德华",
            album = "未知专辑",
            audioUrl = "http://10.0.2.2:8080/static/song_benxiaohai.mp3",
            lyricUrl = "http://10.0.2.2:8080/static/lrc_benxiaohai.lrc"
        ),
        Song(
            id = 7,
            name = "逆浪千秋",
            singer = "言和",
            album = "未知专辑",
            audioUrl = "http://10.0.2.2:8080/static/song_nilang_qianqiu.mp3",
            lyricUrl = "http://10.0.2.2:8080/static/lrc_nilang_qianqiu.lrc"
        )
    )
    
    routing {
        route("/api") {
            get("/songs") {
                call.respond(songs)
            }
        }
        
        // 静态资源路由
        staticResources("/static", "static")
    }
}