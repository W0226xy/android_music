package com.example.myapplication.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log

/**
 * 音频焦点回调接口。
 *
 * PlaybackService 实现此接口以响应焦点变化，
 * 无需直接操作 AudioManager。
 */
interface AudioFocusCallback {

    /** 获得音频焦点，可以开始/恢复播放 */
    fun onFocusGained()

    /** 短暂失去焦点（如导航播报），暂停播放 */
    fun onFocusLostTransient()

    /** 失去焦点但可降低音量继续播放（如地图提示音） */
    fun onFocusLostCanDuck()

    /** 永久失去焦点（如来电），停止播放并释放资源 */
    fun onFocusLost()

    /** 请求音频焦点失败 */
    fun onFocusRequestFailed()
}

/**
 * 音频焦点管理器。
 *
 * 封装 Android [AudioManager] 的焦点 API，
 * 负责请求、释放音频焦点，并将焦点变化
 * 通过 [AudioFocusCallback] 通知 PlaybackService。
 *
 * PlaybackService 不需要直接操作 AudioManager，
 * 只需实现 AudioFocusCallback 并调用本类的
 * [requestFocus] / [abandonFocus]。
 */
class AudioFocusManager(
    context: Context,
    private val callback: AudioFocusCallback
) {

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /** true 表示当前持有音频焦点 */
    var hasFocus: Boolean = false
        private set

    /** 当前焦点是否因为 TRANSIENT 暂时丢失（用于 GAIN 时判断是否恢复） */
    private var transientLoss: Boolean = false

    /**
     * AudioManager 的焦点变化监听器。
     *
     * 将系统回调映射到 [AudioFocusCallback] 的对应方法。
     */
    private val focusChangeListener =
        AudioManager.OnAudioFocusChangeListener { focusChange ->
            Log.d(TAG, "焦点变化: ${focusChangeToString(focusChange)}")

            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    hasFocus = true
                    transientLoss = false
                    callback.onFocusGained()
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    hasFocus = false
                    transientLoss = true
                    callback.onFocusLostTransient()
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    // CAN_DUCK 时仍持有焦点，不需要重新请求
                    callback.onFocusLostCanDuck()
                }

                AudioManager.AUDIOFOCUS_LOSS -> {
                    hasFocus = false
                    transientLoss = false
                    callback.onFocusLost()
                }

                // AUDIOFOCUS_REQUEST_FAILED 不会通过 Listener 回调，
                // 它在 requestAudioFocus 返回值中体现。
                // 此处仅为防御性处理。
                AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                    hasFocus = false
                    transientLoss = false
                    callback.onFocusRequestFailed()
                }

                else -> {
                    Log.w(TAG, "未处理的焦点变化: $focusChange")
                }
            }
        }

    /** 是否在短暂失去焦点后需要自动恢复播放 */
    fun shouldResumeOnGain(): Boolean = transientLoss

    /**
     * 请求音频焦点。
     *
     * @return true 表示成功获取焦点，可以开始播放；
     *         false 表示获取失败，不应播放。
     */
    fun requestFocus(): Boolean {
        if (hasFocus) {
            Log.d(TAG, "已持有焦点，无需重复请求")
            return true
        }

        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requestFocusModern()
        } else {
            requestFocusLegacy()
        }

        hasFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        transientLoss = false

        if (hasFocus) {
            Log.d(TAG, "音频焦点请求成功")
        } else {
            Log.w(TAG, "音频焦点请求失败")
            callback.onFocusRequestFailed()
        }

        return hasFocus
    }

    /**
     * 释放音频焦点。
     *
     * Service 销毁时、或永久失去焦点后调用。
     */
    fun abandonFocus() {
        if (!hasFocus && !transientLoss) {
            Log.d(TAG, "当前未持有焦点，无需释放")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(focusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusChangeListener)
        }

        hasFocus = false
        transientLoss = false
        Log.d(TAG, "音频焦点已释放")
    }

    // ================= API 26+ 现代焦点请求 =================

    private var focusRequest: AudioFocusRequest? = null

    private fun requestFocusModern(): Int {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attrs)
            .setAcceptsDelayedFocusGain(false)   // 不接受延迟焦点
            .setWillPauseWhenDucked(false)       // CAN_DUCK 时我们自己降音量
            .setOnAudioFocusChangeListener(focusChangeListener)
            .build()

        focusRequest = request
        return audioManager.requestAudioFocus(request)
    }

    // ================= API 24–25 兼容焦点请求 =================

    @Suppress("DEPRECATION")
    private fun requestFocusLegacy(): Int {
        return audioManager.requestAudioFocus(
            focusChangeListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
    }

    // ================= 工具方法 =================

    companion object {
        private const val TAG = "AudioFocusManager"

        private fun focusChangeToString(focusChange: Int): String = when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN                     -> "GAIN"
            AudioManager.AUDIOFOCUS_LOSS                     -> "LOSS"
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT           -> "LOSS_TRANSIENT"
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK  -> "LOSS_TRANSIENT_CAN_DUCK"
            AudioManager.AUDIOFOCUS_REQUEST_FAILED           -> "REQUEST_FAILED"
            else                                             -> "UNKNOWN($focusChange)"
        }
    }
}
