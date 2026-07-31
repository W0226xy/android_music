package com.example.myapplication.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.util.LruCache
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.state.updateAppWidgetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import com.example.myapplication.playback.ACTION_PLAYBACK_PROGRESS
import com.example.myapplication.playback.ACTION_PLAYBACK_STATE_CHANGED
import com.example.myapplication.playback.EXTRA_ARTIST
import com.example.myapplication.playback.EXTRA_ARTWORK_URI
import com.example.myapplication.playback.EXTRA_DURATION
import com.example.myapplication.playback.EXTRA_IS_PLAYING
import com.example.myapplication.playback.EXTRA_POSITION
import com.example.myapplication.playback.EXTRA_SONG_NAME

/**
 * 音乐播放器桌面 Widget 的广播接收器。
 *
 * 从 RemoteViews + AppWidgetProvider 迁移到 Jetpack Glance Compose：
 * - UI 改用 [MusicGlanceWidget]（Glance Compose）。
 * - 广播处理 / 封面加载 / WidgetCommandActivity 路径全部保留。
 */
class MusicWidgetProvider : GlanceAppWidgetReceiver() {

    override val glanceAppWidget = MusicGlanceWidget()

    // ── 生命周期 ──

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        widgetCount = 1
        Log.d(TAG, "Widget 已启用")
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        widgetCount = appWidgetIds.size
        Log.d(TAG, "onUpdate: ${appWidgetIds.size}")
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        widgetCount = 0
        artworkCache.evictAll()
        clearArtworkFiles(context)
        Log.d(TAG, "Widget 已全部移除")
    }

    // ── 广播处理 ──

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_PLAYBACK_STATE_CHANGED -> handleStateChanged(context, intent)
            ACTION_PLAYBACK_PROGRESS    -> handleProgress(context, intent)
        }
    }

    private fun handleStateChanged(context: Context, intent: Intent) {
        val songName  = intent.getStringExtra(EXTRA_SONG_NAME) ?: "未在播放"
        val artist    = intent.getStringExtra(EXTRA_ARTIST) ?: ""
        val isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, false)
        val artworkUri= intent.getStringExtra(EXTRA_ARTWORK_URI) ?: ""
        val duration  = intent.getLongExtra(EXTRA_DURATION, 0L)

        Log.d(TAG, "状态更新: $songName - $artist, playing=$isPlaying, widgets=$widgetCount")

        // 1) 立即更新文本信息（封面先置空）
        CoroutineScope(Dispatchers.Main).launch {
            updateAllStates(context, songName, artist, isPlaying, "", duration)
            MusicGlanceWidget().updateAll(context)
        }

        // 2) 异步加载封面 → 局部刷新
        if (artworkUri.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                val artworkPath = loadAndCacheArtwork(context, artworkUri)
                CoroutineScope(Dispatchers.Main).launch {
                    updateAllStates(context, songName, artist, isPlaying,
                        artworkPath, duration)
                    MusicGlanceWidget().updateAll(context)
                }
            }
        }
    }

    private fun handleProgress(context: Context, intent: Intent) {
        val position = intent.getLongExtra(EXTRA_POSITION, 0L)
        val duration = intent.getLongExtra(EXTRA_DURATION, 0L)

        CoroutineScope(Dispatchers.Main).launch {
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(MusicGlanceWidget::class.java)
            ids.forEach { id ->
                updateAppWidgetState(context, id) { prefs ->
                    prefs[WidgetStateKeys.position] = position
                    prefs[WidgetStateKeys.duration] = duration
                }
            }
            MusicGlanceWidget().updateAll(context)
        }
    }

    private suspend fun updateAllStates(
        context: Context, songName: String, artist: String,
        isPlaying: Boolean, artworkPath: String, duration: Long
    ) {
        val manager = GlanceAppWidgetManager(context)
        val ids = manager.getGlanceIds(MusicGlanceWidget::class.java)
        ids.forEach { id ->
            updateAppWidgetState(context, id) { prefs ->
                prefs[WidgetStateKeys.songName] = songName
                prefs[WidgetStateKeys.artist] = artist
                prefs[WidgetStateKeys.isPlaying] = isPlaying
                prefs[WidgetStateKeys.artworkPath] = artworkPath
                prefs[WidgetStateKeys.duration] = duration
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 封面加载 + 文件缓存（与原来完全相同）
    // ═══════════════════════════════════════════════════════════════

    companion object {
        private const val TAG = "MusicWidgetProvider"

        @Volatile var widgetCount = 0

        private val artworkCache = LruCache<String, Bitmap>(5)
        private val artworkFileMap = mutableMapOf<String, String>()

        private fun loadAndCacheArtwork(context: Context, uriStr: String): String {
            artworkFileMap[uriStr]?.let { path ->
                if (File(path).exists()) return path
                artworkFileMap.remove(uriStr)
            }
            val bitmap = loadArtwork(context, uriStr, 256) ?: return ""
            return saveArtworkToFile(context, bitmap, uriStr)
        }

        private fun saveArtworkToFile(context: Context, bitmap: Bitmap, uriKey: String): String {
            val name = "aw_${uriKey.hashCode().toUInt()}.png"
            val file = File(context.cacheDir, name)
            try {
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                }
                artworkFileMap[uriKey] = file.absolutePath
                return file.absolutePath
            } catch (e: Exception) {
                Log.e(TAG, "保存封面文件失败", e)
                return ""
            }
        }

        private fun clearArtworkFiles(context: Context) {
            artworkFileMap.values.forEach { File(it).delete() }
            artworkFileMap.clear()
        }

        private fun loadArtwork(context: Context, uriStr: String, size: Int): Bitmap? {
            if (uriStr.isEmpty()) return null
            artworkCache.get(uriStr)?.let { return it }
            return try {
                val bitmap = when {
                    uriStr.startsWith("http://") || uriStr.startsWith("https://") ->
                        loadNetworkBitmap(uriStr, size)
                    else -> loadLocalBitmap(context, uriStr, size)
                }
                bitmap?.let { artworkCache.put(uriStr, it) }
                bitmap
            } catch (e: Exception) {
                Log.e(TAG, "加载封面失败: $uriStr", e)
                null
            }
        }

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
}
