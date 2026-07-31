package com.example.myapplication.playback

const val ACTION_OPEN_PLAYER =
    "com.example.myapplication.action.OPEN_PLAYER"

// ================= Widget 状态广播 =================

const val ACTION_PLAYBACK_STATE_CHANGED =
    "com.example.myapplication.widget.PLAYBACK_STATE_CHANGED"

const val ACTION_PLAYBACK_PROGRESS =
    "com.example.myapplication.widget.PLAYBACK_PROGRESS"

// ================= Widget 按钮命令 =================

const val ACTION_WIDGET_PLAY     = "com.example.myapplication.widget.PLAY"
const val ACTION_WIDGET_PAUSE    = "com.example.myapplication.widget.PAUSE"
const val ACTION_WIDGET_NEXT     = "com.example.myapplication.widget.NEXT"
const val ACTION_WIDGET_PREVIOUS = "com.example.myapplication.widget.PREVIOUS"
const val ACTION_WIDGET_SEEK_FWD = "com.example.myapplication.widget.SEEK_FWD"
const val ACTION_WIDGET_SEEK_BACK= "com.example.myapplication.widget.SEEK_BACK"

// ================= Extra Keys =================

const val EXTRA_SONG_NAME   = "song_name"
const val EXTRA_ARTIST      = "artist"
const val EXTRA_IS_PLAYING  = "is_playing"
const val EXTRA_ARTWORK_URI = "artwork_uri"
const val EXTRA_POSITION    = "position"
const val EXTRA_DURATION    = "duration"

// ================= 工具 =================

fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val min = total / 60
    val sec = total % 60
    return "%02d:%02d".format(min, sec)
}
