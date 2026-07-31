package com.example.myapplication.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.util.LruCache
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import java.net.HttpURLConnection
import java.net.URL
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.playback.ACTION_OPEN_PLAYER
import com.example.myapplication.playback.ACTION_PLAYBACK_PROGRESS
import com.example.myapplication.playback.ACTION_PLAYBACK_STATE_CHANGED
import com.example.myapplication.playback.ACTION_WIDGET_NEXT
import com.example.myapplication.playback.ACTION_WIDGET_PAUSE
import com.example.myapplication.playback.ACTION_WIDGET_PLAY
import com.example.myapplication.playback.ACTION_WIDGET_PREVIOUS
import com.example.myapplication.playback.ACTION_WIDGET_SEEK_BACK
import com.example.myapplication.playback.ACTION_WIDGET_SEEK_FWD
import com.example.myapplication.playback.EXTRA_ARTIST
import com.example.myapplication.playback.EXTRA_ARTWORK_URI
import com.example.myapplication.playback.EXTRA_DURATION
import com.example.myapplication.playback.EXTRA_IS_PLAYING
import com.example.myapplication.playback.EXTRA_POSITION
import com.example.myapplication.playback.EXTRA_SONG_NAME
import com.example.myapplication.playback.formatTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "MusicWidgetProvider"

        @Volatile var widgetCount = 0

        private val artworkCache = LruCache<String, Bitmap>(5)

        /**
         * 加载封面 Bitmap，支持本地（content:// / android.resource://）
         * 和网络（http:// / https://）两种来源。
         */
        private fun loadArtwork(context: Context, uriStr: String, size: Int): Bitmap? {
            if (uriStr.isEmpty()) return null
            val cached = artworkCache.get(uriStr)
            if (cached != null) return cached

            return try {
                val bitmap = when {
                    uriStr.startsWith("http://") || uriStr.startsWith("https://") ->
                        loadNetworkBitmap(uriStr, size)
                    else ->
                        loadLocalBitmap(context, uriStr, size)
                }
                bitmap?.let { artworkCache.put(uriStr, it) }
                bitmap
            } catch (e: Exception) {
                Log.e(TAG, "加载封面失败: $uriStr", e)
                null
            }
        }

        /**
         * 从本地 URI（content:// / android.resource:// / file://）加载封面。
         */
        private fun loadLocalBitmap(context: Context, uriStr: String, size: Int): Bitmap? {
            return try {
                val uri = Uri.parse(uriStr)
                val input = context.contentResolver.openInputStream(uri) ?: return null
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(input, null, opts)
                input.close()

                opts.inSampleSize = calculateSampleSize(opts.outWidth, opts.outHeight, size)
                opts.inJustDecodeBounds = false

                val input2 = context.contentResolver.openInputStream(uri) ?: return null
                BitmapFactory.decodeStream(input2, null, opts).also { input2.close() }
            } catch (e: Exception) {
                Log.e(TAG, "加载本地封面失败: $uriStr", e)
                null
            }
        }

        /**
         * 从网络 URL 下载并解码封面图片。
         * 先下载到内存，再分两次解码（bounds → full），避免 OOM。
         */
        private fun loadNetworkBitmap(urlStr: String, size: Int): Bitmap? {
            return try {
                val conn = URL(urlStr).openConnection() as HttpURLConnection
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                conn.doInput = true
                conn.connect()

                if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                    Log.w(TAG, "网络封面 HTTP ${conn.responseCode}: $urlStr")
                    return null
                }

                val bytes = conn.inputStream.use { it.readBytes() }
                conn.disconnect()

                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)

                opts.inSampleSize = calculateSampleSize(opts.outWidth, opts.outHeight, size)
                opts.inJustDecodeBounds = false

                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            } catch (e: Exception) {
                Log.e(TAG, "加载网络封面失败: $urlStr", e)
                null
            }
        }

        private fun calculateSampleSize(w: Int, h: Int, target: Int): Int {
            var size = 1
            while (w / size > target || h / size > target) size *= 2
            return size
        }
    }

    // ── 生命周期 ──

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        widgetCount = 1
        Log.d(TAG, "Widget 已启用")
    }

    override fun onUpdate(context: Context, awm: AppWidgetManager, ids: IntArray) {
        Log.d(TAG, "onUpdate: ${ids.size}")
        widgetCount = ids.size
        for (id in ids) updateWidget(context, awm, id, "未在播放", "", false, "", 0L)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        widgetCount = 0
        artworkCache.evictAll()
        Log.d(TAG, "Widget 已全部移除")
    }

    // ── 广播处理 ──

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val awm = AppWidgetManager.getInstance(context)
        val cn   = ComponentName(context, MusicWidgetProvider::class.java)
        val ids  = awm.getAppWidgetIds(cn)
        widgetCount = ids.size

        when (intent.action) {
            ACTION_PLAYBACK_STATE_CHANGED -> {
                val songName  = intent.getStringExtra(EXTRA_SONG_NAME) ?: "未在播放"
                val artist    = intent.getStringExtra(EXTRA_ARTIST) ?: ""
                val isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, false)
                val artwork   = intent.getStringExtra(EXTRA_ARTWORK_URI) ?: ""
                val duration  = intent.getLongExtra(EXTRA_DURATION, 0L)

                Log.d(TAG, "状态更新: $songName - $artist, playing=$isPlaying, widgets=${ids.size}")

                for (id in ids) {
                    updateWidget(context, awm, id, songName, artist, isPlaying, artwork, duration)
                }
            }

            ACTION_PLAYBACK_PROGRESS -> {
                val position = intent.getLongExtra(EXTRA_POSITION, 0L)
                val duration = intent.getLongExtra(EXTRA_DURATION, 0L)
                for (id in ids) updateProgress(context, awm, id, position, duration)
            }
        }
    }

    // ── Widget 更新 ──

    private fun updateWidget(
        context: Context, awm: AppWidgetManager, id: Int,
        songName: String, artist: String, isPlaying: Boolean,
        artworkUri: String, duration: Long
    ) {
        val views = RemoteViews(context.packageName, R.layout.music_widget)

        // 歌曲信息
        views.setTextViewText(R.id.widget_song_name, songName)
        views.setTextViewText(R.id.widget_artist, artist)

        // 时长
        views.setTextViewText(R.id.widget_time_total, formatTime(duration))
        views.setTextViewText(R.id.widget_time_current, "00:00")
        views.setProgressBar(R.id.widget_progress,
            duration.coerceAtLeast(1L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), 0, false)

        // 播放/暂停图标与命令
        val playAction = if (isPlaying) ACTION_WIDGET_PAUSE else ACTION_WIDGET_PLAY
        views.setImageViewResource(R.id.widget_btn_play_pause,
            if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play)
        views.setOnClickPendingIntent(R.id.widget_btn_play_pause,
            makePi(context, id, playAction))
        views.setOnClickPendingIntent(R.id.widget_btn_next,
            makePi(context, id, ACTION_WIDGET_NEXT))
        views.setOnClickPendingIntent(R.id.widget_btn_previous,
            makePi(context, id, ACTION_WIDGET_PREVIOUS))
        views.setOnClickPendingIntent(R.id.widget_btn_forward,
            makePi(context, id, ACTION_WIDGET_SEEK_FWD))
        views.setOnClickPendingIntent(R.id.widget_btn_rewind,
            makePi(context, id, ACTION_WIDGET_SEEK_BACK))

        // 点击打开 App
        val openPi = PendingIntent.getActivity(context, id * 10 + 4,
            Intent(context, MainActivity::class.java).apply {
                action = ACTION_OPEN_PLAYER
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.widget_root, openPi)

        // ── 封面 ──
        if (artworkUri.isNotEmpty()) {
            // 有封面 URL：先显示占位图，异步加载真实封面
            views.setInt(R.id.widget_album_art, "setVisibility", View.VISIBLE)
            views.setImageViewResource(R.id.widget_album_art, R.drawable.ic_widget_default_album)
            // 先同步应用非封面字段，避免被异步加载阻滞
            awm.updateAppWidget(id, views)

            CoroutineScope(Dispatchers.IO).launch {
                val bitmap = loadArtwork(context, artworkUri, 256)
                withContext(Dispatchers.Main) {
                    // 局部更新：仅覆盖封面 ImageView，不影响其他字段
                    val artViews = RemoteViews(context.packageName, R.layout.music_widget)
                    if (bitmap != null) {
                        artViews.setInt(R.id.widget_album_art, "setVisibility", View.VISIBLE)
                        artViews.setImageViewBitmap(R.id.widget_album_art, bitmap)
                    } else {
                        // 加载失败 → 空白
                        artViews.setInt(R.id.widget_album_art, "setVisibility", View.GONE)
                    }
                    awm.partiallyUpdateAppWidget(id, artViews)
                }
            }
        } else {
            // 无封面 → 空白
            views.setInt(R.id.widget_album_art, "setVisibility", View.GONE)
            awm.updateAppWidget(id, views)
        }
    }

    private fun updateProgress(
        context: Context, awm: AppWidgetManager, id: Int,
        position: Long, duration: Long
    ) {
        val views = RemoteViews(context.packageName, R.layout.music_widget)
        views.setProgressBar(R.id.widget_progress,
            duration.coerceAtLeast(1L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            position.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), false)
        views.setTextViewText(R.id.widget_time_current, formatTime(position))
        views.setTextViewText(R.id.widget_time_total, formatTime(duration))
        // 局部更新 — 不重建 PendingIntent
        awm.partiallyUpdateAppWidget(id, views)
    }

    private fun makePi(context: Context, widgetId: Int, action: String): PendingIntent {
        val intent = Intent(context, WidgetCommandActivity::class.java).apply {
            this.action = action
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
        }
        val req = widgetId * 100 + 50 + (kotlin.math.abs(action.hashCode()) % 10)
        return PendingIntent.getActivity(context, req, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
