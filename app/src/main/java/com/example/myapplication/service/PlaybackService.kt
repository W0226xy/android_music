package com.example.myapplication.service

import android.app.PendingIntent
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

class PlaybackService : MediaSessionService(), AudioFocusCallback {

    // ================= 音频焦点管理 =================

    private var audioFocusManager: AudioFocusManager? = null

    private var player: ExoPlayer? = null

    /** 被 CAN_DUCK 降音量前的原始音量，恢复焦点后还原 */
    private var volumeBeforeDuck: Float = 1.0f

    /** 当前是否处于 CAN_DUCK 降音状态 */
    private var isDucked: Boolean = false

    private var mediaSession: MediaSession? = null

    // ---- 防抖通知 ----
    private val notifyHandler = Handler(Looper.getMainLooper())
    private var notifyPending = false

    // ---- 进度定时器 ----
    private val progressHandler = Handler(Looper.getMainLooper())
    private var progressRunning = false

    private val progressRunnable = object : Runnable {
        override fun run() {
            val p = mediaSession?.player ?: return
            if (!p.isPlaying || MusicWidgetProvider.widgetCount == 0) {
                progressRunning = false
                return
            }
            notifyProgress(p.currentPosition, p.duration)
            progressHandler.postDelayed(this, 1000L)
        }
    }

    // ================= 播放器监听器 =================

    /**
     * 焦点感知监听器 —— 最早注册，最先收到回调。
     *
     * 当用户通过 MediaController / MediaSession 触发播放时，
     * [onPlayWhenReadyChanged] 会先于 ExoPlayer 实际出声被调用，
     * 此时检查音频焦点：未持有则申请，申请失败则阻止播放。
     */
    private val focusPlayerListener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (playWhenReady) {
                // 用户按下播放 → 检查音频焦点
                val focusOk = audioFocusManager?.let { mgr ->
                    mgr.hasFocus || mgr.requestFocus()
                } ?: true

                if (!focusOk) {
                    Log.w(TAG, "音频焦点请求失败，阻止播放")
                    player?.playWhenReady = false
                }
            }
        }
    }

    /**
     * Widget 状态监听器 —— 仅次于焦点监听器注册。
     *
     * 当歌曲切换或播放状态变化时，通知桌面 Widget 刷新。
     */
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

    // ================= 音频焦点回调实现 =================

    /**
     * 重新获得音频焦点。
     *
     * 可能发生在：
     * - 导航播报结束
     * - 电话挂断后用户切回 App
     *
     * 如果之前是 TRANSIENT 暂时失去焦点，自动恢复播放。
     * 如果之前被 CAN_DUCK 降低了音量，恢复原始音量。
     */
    override fun onFocusGained() {
        Log.d(TAG, "AudioFocus 恢复 (GAIN)")

        // 恢复被 duck 降低的音量
        if (isDucked) {
            player?.volume = volumeBeforeDuck
            isDucked = false
        }

        // 短暂失去焦点后自动恢复播放
        if (audioFocusManager?.shouldResumeOnGain() == true) {
            player?.play()
        }
    }

    /**
     * 其他应用短暂抢占音频焦点（如导航播报）。
     *
     * 策略：暂停播放，等待焦点恢复后自动继续。
     */
    override fun onFocusLostTransient() {
        Log.d(TAG, "AudioFocus 短暂失去 (LOSS_TRANSIENT) → 暂停")

        // 先恢复 duck 音量再暂停（避免下次 play 时音量仍是 0.2）
        if (isDucked) {
            player?.volume = volumeBeforeDuck
            isDucked = false
        }

        player?.pause()
    }

    /**
     * 其他应用需要以较低音量播放（如地图语音提示）。
     *
     * 策略：将 ExoPlayer 音量降至 20% 继续播放，不清除焦点。
     */
    override fun onFocusLostCanDuck() {
        Log.d(TAG, "AudioFocus 被抢占但可降低音量 (LOSS_TRANSIENT_CAN_DUCK)")

        if (!isDucked) {
            volumeBeforeDuck = player?.volume ?: 1.0f
            player?.volume = 0.2f
            isDucked = true
        }
    }

    /**
     * 永久失去音频焦点（如来电）。
     *
     * 策略：停止播放并主动释放焦点，不自动恢复。
     */
    override fun onFocusLost() {
        Log.d(TAG, "AudioFocus 永久失去 (LOSS) → 停止并释放")

        // 恢复音量
        if (isDucked) {
            player?.volume = volumeBeforeDuck
            isDucked = false
        }

        player?.stop()
        audioFocusManager?.abandonFocus()
    }

    /**
     * 音频焦点请求被系统拒绝。
     *
     * 此时 [focusPlayerListener.onPlayWhenReadyChanged] 已将
     * playWhenReady 设回 false，这里仅记录日志。
     */
    override fun onFocusRequestFailed() {
        Log.w(TAG, "AudioFocus 请求被系统拒绝")
    }

    // ================= Widget 通知辅助方法 =================

    private fun startProgressIfNeeded() {
        if (progressRunning) return
        if (MusicWidgetProvider.widgetCount == 0) return
        val p = mediaSession?.player ?: return
        if (!p.isPlaying) return
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
        val p = mediaSession?.player ?: return
        val mediaItem = p.currentMediaItem

        val songName = mediaItem?.mediaMetadata?.title?.toString()
            ?: mediaItem?.mediaMetadata?.displayTitle?.toString()
            ?: "未在播放"
        val artist   = mediaItem?.mediaMetadata?.artist?.toString() ?: ""
        val artwork  = mediaItem?.mediaMetadata?.artworkUri?.toString() ?: ""
        val playing  = p.isPlaying

        Log.d(TAG, "通知 Widget: $songName - $artist, playing=$playing, art=$artwork")

        val intent = Intent(this, MusicWidgetProvider::class.java).apply {
            action = ACTION_PLAYBACK_STATE_CHANGED
            putExtra(EXTRA_SONG_NAME,   songName)
            putExtra(EXTRA_ARTIST,      artist)
            putExtra(EXTRA_IS_PLAYING,  playing)
            putExtra(EXTRA_ARTWORK_URI, artwork)
            putExtra(EXTRA_DURATION,    p.duration.takeIf { it != C.TIME_UNSET } ?: 0L)
            putExtra(EXTRA_POSITION,    p.currentPosition)
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

    // ================= 生命周期 =================

    override fun onCreate() {
        super.onCreate()

        // 1. 创建 ExoPlayer
        //    handleAudioFocus = false：关闭 Media3 内置自动焦点管理，
        //    改由 AudioFocusManager 接管，以便精确控制
        //    CAN_DUCK 降音、TRANSIENT 暂停、LOSS 停止等行为。
        val exoPlayer = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                false   // ← 关键：关闭 ExoPlayer 内置音频焦点处理
            )
            setHandleAudioBecomingNoisy(true)

            // 焦点监听器先注册，Widget 监听器后注册
            addListener(focusPlayerListener)
            addListener(widgetStateListener)
        }
        player = exoPlayer

        // 2. 创建 AudioFocusManager
        audioFocusManager = AudioFocusManager(this, this)

        // 3. 创建 MediaSession
        val openIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_OPEN_PLAYER
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(
                PendingIntent.getActivity(
                    this, 1001, openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

        Log.d(TAG, "PlaybackService 创建完成")
    }

    override fun onGetSession(info: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        Log.d(TAG, "PlaybackService 销毁")

        // 停止进度与通知定时器
        progressHandler.removeCallbacks(progressRunnable)
        notifyHandler.removeCallbacksAndMessages(null)

        // 释放音频焦点
        audioFocusManager?.abandonFocus()
        audioFocusManager = null

        // 移除监听器
        mediaSession?.player?.removeListener(widgetStateListener)
        mediaSession?.player?.removeListener(focusPlayerListener)

        // 释放 ExoPlayer 与 MediaSession
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        player = null

        super.onDestroy()
    }

    companion object {
        private const val TAG = "PlaybackService"
    }
}
