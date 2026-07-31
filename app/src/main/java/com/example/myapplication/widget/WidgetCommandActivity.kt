package com.example.myapplication.widget

import android.app.Activity
import android.content.ComponentName
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.myapplication.playback.ACTION_WIDGET_NEXT
import com.example.myapplication.playback.ACTION_WIDGET_PAUSE
import com.example.myapplication.playback.ACTION_WIDGET_PLAY
import com.example.myapplication.playback.ACTION_WIDGET_PREVIOUS
import com.example.myapplication.playback.ACTION_WIDGET_SEEK_BACK
import com.example.myapplication.playback.ACTION_WIDGET_SEEK_FWD
import com.example.myapplication.service.PlaybackService

class WidgetCommandActivity : Activity() {

    companion object { private const val TAG = "WidgetCmdActivity" }
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val action = intent?.action ?: run { finish(); return }

        Log.d(TAG, "onCreate: $action")

        val sessionToken = SessionToken(this,
            ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, sessionToken).buildAsync()

        future.addListener({
            var ctrl: MediaController? = null
            try {
                ctrl = future.get()
                if (ctrl.mediaItemCount == 0) {
                    Log.w(TAG, "播放列表为空")
                    return@addListener
                }

                Log.d(TAG, "items=${ctrl.mediaItemCount}, playing=${ctrl.isPlaying}")

                when (action) {
                    ACTION_WIDGET_PLAY  -> ctrl.play()
                    ACTION_WIDGET_PAUSE -> ctrl.pause()

                    ACTION_WIDGET_NEXT -> {
                        val wasPlaying = ctrl.isPlaying
                        ctrl.seekToNextMediaItem()
                        if (wasPlaying) ctrl.play()
                    }

                    ACTION_WIDGET_PREVIOUS -> {
                        val wasPlaying = ctrl.isPlaying
                        if (ctrl.currentPosition > 3000) ctrl.seekTo(0)
                        else ctrl.seekToPreviousMediaItem()
                        if (wasPlaying) ctrl.play()
                    }

                    ACTION_WIDGET_SEEK_FWD -> {
                        val target = ctrl.currentPosition + 10_000
                        val max = ctrl.duration.takeIf { it > 0 } ?: target
                        ctrl.seekTo(target.coerceAtMost(max))
                    }

                    ACTION_WIDGET_SEEK_BACK -> {
                        val target = ctrl.currentPosition - 10_000
                        ctrl.seekTo(target.coerceAtLeast(0))
                    }
                }
                Log.d(TAG, "命令完成: $action")
            } catch (e: Exception) {
                Log.e(TAG, "失败: ${e.message}", e)
            } finally {
                val f = future; val c = ctrl
                handler.postDelayed({
                    c?.release()
                    MediaController.releaseFuture(f)
                    finish()
                }, 300L)
            }
        }, { r -> r.run() })
    }
}
