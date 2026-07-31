package com.example.myapplication.service

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.myapplication.MainActivity
import com.example.myapplication.playback.ACTION_OPEN_PLAYER
import com.example.myapplication.playback.ACTION_PLAYBACK_PROGRESS
import com.example.myapplication.playback.ACTION_PLAYBACK_STATE_CHANGED
import com.example.myapplication.playback.EXTRA_ARTIST
import com.example.myapplication.playback.EXTRA_ARTWORK_URI
import com.example.myapplication.playback.EXTRA_DURATION
import com.example.myapplication.playback.EXTRA_IS_PLAYING
import com.example.myapplication.playback.EXTRA_POSITION
import com.example.myapplication.playback.EXTRA_SONG_NAME
import com.example.myapplication.widget.MusicWidgetProvider

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    // ---- 防抖通知 ----
    private val notifyHandler = Handler(Looper.getMainLooper())
    private var notifyPending = false

    // ---- 进度定时器 ----
    private val progressHandler = Handler(Looper.getMainLooper())
    private var progressRunning = false

    private val progressRunnable = object : Runnable {
        override fun run() {
            val player = mediaSession?.player
            if (player == null || !player.isPlaying || MusicWidgetProvider.widgetCount == 0) {
                progressRunning = false
                return
            }
            notifyProgress(player.currentPosition, player.duration)
            progressHandler.postDelayed(this, 1000L)
        }
    }

    private val widgetStateListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            scheduleNotify()
            startProgressIfNeeded()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            scheduleNotify()
            if (isPlaying) startProgressIfNeeded()
        }
    }

    private fun startProgressIfNeeded() {
        if (progressRunning) return
        if (MusicWidgetProvider.widgetCount == 0) return
        val player = mediaSession?.player ?: return
        if (!player.isPlaying) return
        progressRunning = true
        progressHandler.post(progressRunnable)
    }

    private fun scheduleNotify() {
        if (notifyPending) return
        notifyPending = true
        notifyHandler.postDelayed({
            notifyPending = false
            notifyWidget()
        }, 150L)
    }

    private fun notifyWidget() {
        val player = mediaSession?.player ?: return
        val mediaItem = player.currentMediaItem

        val songName = mediaItem?.mediaMetadata?.title?.toString()
            ?: mediaItem?.mediaMetadata?.displayTitle?.toString()
            ?: "未在播放"
        val artist   = mediaItem?.mediaMetadata?.artist?.toString() ?: ""
        val artwork  = mediaItem?.mediaMetadata?.artworkUri?.toString() ?: ""
        val playing  = player.isPlaying

        Log.d("PlaybackService", "通知 Widget: $songName - $artist, playing=$playing, art=$artwork")

        val intent = Intent(this, MusicWidgetProvider::class.java).apply {
            action = ACTION_PLAYBACK_STATE_CHANGED
            putExtra(EXTRA_SONG_NAME,   songName)
            putExtra(EXTRA_ARTIST,      artist)
            putExtra(EXTRA_IS_PLAYING,  playing)
            putExtra(EXTRA_ARTWORK_URI, artwork)
            putExtra(EXTRA_DURATION,    player.duration.takeIf { it != C.TIME_UNSET } ?: 0L)
            putExtra(EXTRA_POSITION,    player.currentPosition)
        }
        sendBroadcast(intent)
    }

    private fun notifyProgress(position: Long, duration: Long) {
        val intent = Intent(this, MusicWidgetProvider::class.java).apply {
            action = ACTION_PLAYBACK_PROGRESS
            putExtra(EXTRA_POSITION, position)
            putExtra(EXTRA_DURATION, duration)
        }
        sendBroadcast(intent)
    }

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            setHandleAudioBecomingNoisy(true)
            addListener(widgetStateListener)
        }

        val openIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_OPEN_PLAYER
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(PendingIntent.getActivity(this, 1001, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .build()
    }

    override fun onGetSession(info: MediaSession.ControllerInfo) = mediaSession

    override fun onDestroy() {
        progressHandler.removeCallbacks(progressRunnable)
        notifyHandler.removeCallbacksAndMessages(null)
        mediaSession?.player?.removeListener(widgetStateListener)
        mediaSession?.run { player.release(); release() }
        mediaSession = null
        super.onDestroy()
    }
}
