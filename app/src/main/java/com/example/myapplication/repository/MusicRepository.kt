package com.example.myapplication.repository

import com.example.myapplication.R
import com.example.myapplication.data.Song

class MusicRepository {

    fun getSongs(): List<Song> {
        return listOf(
            Song(
                id = 1,
                name = "2002年的第一场雪",
                singer = "刀郎",
                album = "未知专辑",
                audioResId = R.raw.song_2002_first_snow,
                lyricResId = R.raw.lrc_2002_first_snow
            ),
            Song(
                id = 2,
                name = "Andy",
                singer = "阿杜",
                album = "未知专辑",
                audioResId = R.raw.song_andy_adu,
                lyricResId = R.raw.lrc_andy_adu
            ),
            Song(
                id = 3,
                name = "别说我的眼泪你无所谓",
                singer = "东来东往",
                album = "未知专辑",
                audioResId = R.raw.song_tears,
                lyricResId = R.raw.lrc_tears
            ),
            Song(
                id = 4,
                name = "时间你慢些走",
                singer = "未知歌手",
                album = "未知专辑",
                audioResId = R.raw.song_time_slow,
                lyricResId = R.raw.lrc_time_slow
            ),
            Song(
                id = 5,
                name = "爱是你我",
                singer = "刀郎 / 云朵 / 王翰仪",
                album = "未知专辑",
                audioResId = R.raw.song_love_you_me,
                lyricResId = R.raw.lrc_love_you_me
            ),
            Song(
                id = 6,
                name = "笨小孩",
                singer = "刘德华",
                album = "未知专辑",
                audioResId = R.raw.song_benxiaohai,
                lyricResId = R.raw.lrc_benxiaohai
            ),
            Song(
                id = 7,
                name = "逆浪千秋",
                singer = "言和",
                album = "未知专辑",
                audioResId = R.raw.song_nilang_qianqiu,
                lyricResId = R.raw.lrc_nilang_qianqiu
            )
        )
    }
}