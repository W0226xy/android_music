package com.example.myapplication.service

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.myapplication.MainActivity
import com.example.myapplication.playback.ACTION_OPEN_PLAYER

/**
 * 真正持有播放器的后台服务。
 *
 * MainActivity 退出前台后，ExoPlayer 仍然由这个 Service 持有，
 * 所以音乐可以继续播放。
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(this)
            .build()
            .apply {
                // 自动申请和释放音频焦点
                setAudioAttributes(
                    audioAttributes,
                    true
                )

                // 拔出有线耳机或断开音频设备时自动暂停
                setHandleAudioBecomingNoisy(true)
            }

        val openPlayerIntent = Intent(
            this,
            MainActivity::class.java
        ).apply {
            action = ACTION_OPEN_PLAYER
            flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val sessionActivity = PendingIntent.getActivity(
            this,
            1001,
            openPlayerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(
            this,
            player
        )
            .setSessionActivity(sessionActivity)
            .build()
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }

        mediaSession = null
        super.onDestroy()
    }
}