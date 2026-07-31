package com.example.myapplication.widget

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.playback.ACTION_OPEN_PLAYER
import com.example.myapplication.playback.ACTION_WIDGET_NEXT
import com.example.myapplication.playback.ACTION_WIDGET_PAUSE
import com.example.myapplication.playback.ACTION_WIDGET_PLAY
import com.example.myapplication.playback.ACTION_WIDGET_PREVIOUS

// ══ Intent ══

private fun cmd(context: Context, action: String) =
    Intent(context, WidgetCommandActivity::class.java).apply {
        this.action = action
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
    }

private fun openApp(context: Context) =
    Intent(context, MainActivity::class.java).apply {
        action = ACTION_OPEN_PLAYER
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }

// ══ GlanceAppWidget ══

class MusicGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme { MusicWidget(context) }
        }
    }
}

// ═══════════════════════════════════════════════════════
// 主布局  QQ 音乐风格：横向 封面 | 歌名/歌手/按钮
// 可用 ≈ 260×90 (280×110 - 10padding)
// ═══════════════════════════════════════════════════════

@Composable
private fun MusicWidget(context: Context) {
    val songName    = currentState(WidgetStateKeys.songName) ?: "未在播放"
    val artist      = currentState(WidgetStateKeys.artist) ?: ""
    val isPlaying   = currentState(WidgetStateKeys.isPlaying) ?: false
    val artworkPath = currentState(WidgetStateKeys.artworkPath) ?: ""

    // Box 填充整个 Widget，内部居中
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(12.dp)
            .clickable(actionStartActivity(openApp(context))),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── 左侧：封面 60dp ──
            AlbumArt(artworkPath)
            Spacer(modifier = GlanceModifier.width(12.dp))
            // ── 右侧：歌名 + 歌手 + 按钮 ──
            InfoAndControls(context, songName, artist, isPlaying)
        }
    }
}

// ═══════ 封面 ═══════

@Composable
private fun AlbumArt(artworkPath: String) {
    val provider: ImageProvider? = when {
        artworkPath.isNotEmpty() -> try {
            BitmapFactory.decodeFile(artworkPath)?.let { ImageProvider(it) }
        } catch (_: Exception) { null }
        else -> null
    }

    Box(
        modifier = GlanceModifier.size(60.dp).cornerRadius(6.dp)
            .background(GlanceTheme.colors.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (provider != null) Image(provider = provider, contentDescription = null)
    }
}

// ═══════ 右侧：信息 + 控制 ═══════

@Composable
private fun InfoAndControls(
    context: Context, songName: String, artist: String, isPlaying: Boolean
) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        // 歌名
        Text(
            text = songName,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        // 歌手
        Text(
            text = artist.ifEmpty { " " },
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 12.sp
            ),
            maxLines = 1
        )
        Spacer(modifier = GlanceModifier.height(10.dp))
        // 控制按钮行  (可用宽 ≈ 188dp, 3 × 60 = 180)
        ControlRow(context, isPlaying)
    }
}

// ═══════ 控制按钮 ═══════

@Composable
private fun ControlRow(context: Context, isPlaying: Boolean) {
    val action = if (isPlaying) ACTION_WIDGET_PAUSE else ACTION_WIDGET_PLAY
    val icon   = if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play

    Row(
        modifier = GlanceModifier.fillMaxWidth().height(36.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Btn(context, 60.dp, R.drawable.ic_widget_previous, ACTION_WIDGET_PREVIOUS)
        Btn(context, 60.dp, icon, action)
        Btn(context, 60.dp, R.drawable.ic_widget_next, ACTION_WIDGET_NEXT)
    }
}

@Composable
private fun Btn(context: Context, w: androidx.compose.ui.unit.Dp, icon: Int, action: String) {
    Box(
        modifier = GlanceModifier.width(w).height(36.dp)
            .clickable(actionStartActivity(cmd(context, action))),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface)
        )
    }
}
