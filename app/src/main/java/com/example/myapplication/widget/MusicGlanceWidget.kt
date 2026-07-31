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
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.ProgressIndicatorDefaults
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
import com.example.myapplication.playback.ACTION_WIDGET_SEEK_BACK
import com.example.myapplication.playback.ACTION_WIDGET_SEEK_FWD
import com.example.myapplication.playback.formatTime

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

// ═════════════════════════════════════════════════════════════
// 完整播放器卡片  300×175dp
// 外层 8dp padding → 卡片 284×159 → 卡片内 264×139 (10dp padding)
// ═════════════════════════════════════════════════════════════

@Composable
private fun MusicWidget(context: Context) {
    val songName    = currentState(WidgetStateKeys.songName) ?: "未在播放"
    val artist      = currentState(WidgetStateKeys.artist) ?: ""
    val isPlaying   = currentState(WidgetStateKeys.isPlaying) ?: false
    val artworkPath = currentState(WidgetStateKeys.artworkPath) ?: ""
    val duration    = currentState(WidgetStateKeys.duration) ?: 0L
    val position    = currentState(WidgetStateKeys.position) ?: 0L

    // 外层居中
    Box(
        modifier = GlanceModifier.fillMaxSize().padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        // 卡片
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .cornerRadius(16.dp)
                .background(GlanceTheme.colors.surface)
                .padding(12.dp)
                .clickable(actionStartActivity(openApp(context)))
        ) {
            // ── 顶部：封面 + 歌曲信息 ──
            HeaderRow(artworkPath, songName, artist)
            SpacerH(8)
            // ── 中部：进度条 ──
            ProgressRow(position, duration)
            SpacerH(6)
            // ── 底部：5 控制按钮 ──
            ControlRow(context, isPlaying)
        }
    }
}

@Composable private fun SpacerH(h: Int) = Spacer(modifier = GlanceModifier.height(h.dp))

// ═══════════════════ 顶部：封面 56 + 间距 10 + 文字 ═══════════════════

@Composable
private fun HeaderRow(artworkPath: String, songName: String, artist: String) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 封面 56dp 圆角
        AlbumArt(artworkPath)
        Spacer(modifier = GlanceModifier.width(10.dp))
        // 歌名 + 歌手
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = songName,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            SpacerH(2)
            Text(
                text = artist.ifEmpty { " " },
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 12.sp),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AlbumArt(artworkPath: String) {
    val provider: ImageProvider? = when {
        artworkPath.isNotEmpty() -> try {
            BitmapFactory.decodeFile(artworkPath)?.let { ImageProvider(it) }
        } catch (_: Exception) { null }
        else -> null
    }
    Box(
        modifier = GlanceModifier.size(56.dp).cornerRadius(8.dp)
            .background(GlanceTheme.colors.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (provider != null) Image(provider = provider, contentDescription = null)
    }
}

// ═══════════════════ 中部：时间 + 进度条 + 时间 ═══════════════════

@Composable
private fun ProgressRow(position: Long, duration: Long) {
    val progress = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f

    Row(
        modifier = GlanceModifier.fillMaxWidth().height(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 当前时间
        Text(
            text = formatTime(position),
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 11.sp),
            modifier = GlanceModifier.width(34.dp)
        )
        // 进度条
        Box(
            modifier = GlanceModifier.fillMaxWidth().height(24.dp).padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            LinearProgressIndicator(
                progress = progress,
                modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                color = ProgressIndicatorDefaults.IndicatorColorProvider,
                backgroundColor = ProgressIndicatorDefaults.BackgroundColorProvider
            )
        }
        // 总时长
        Text(
            text = formatTime(duration),
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 11.sp),
            modifier = GlanceModifier.width(34.dp)
        )
    }
}

// ═══════════════════ 底部：5 控制按钮 ═══════════════════
// ⏮  ◁◁  ▶/⏸  ▷▷  ⏭

@Composable
private fun ControlRow(context: Context, isPlaying: Boolean) {
    val ppAction = if (isPlaying) ACTION_WIDGET_PAUSE else ACTION_WIDGET_PLAY
    val ppIcon   = if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play

    Row(
        modifier = GlanceModifier.fillMaxWidth().height(42.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 上一首 ⏮
        IconBtn(context, 52.dp, R.drawable.ic_widget_previous, ACTION_WIDGET_PREVIOUS)
        // 后退10s ◁◁
        IconBtn(context, 52.dp, R.drawable.ic_widget_rewind, ACTION_WIDGET_SEEK_BACK)
        // 播放/暂停 ▶/⏸ (绿色高亮)
        Box(
            modifier = GlanceModifier.width(52.dp).height(42.dp)
                .clickable(actionStartActivity(cmd(context, ppAction))),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(ppIcon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(ProgressIndicatorDefaults.IndicatorColorProvider)
            )
        }
        // 前进10s ▷▷
        IconBtn(context, 52.dp, R.drawable.ic_widget_forward, ACTION_WIDGET_SEEK_FWD)
        // 下一首 ⏭
        IconBtn(context, 52.dp, R.drawable.ic_widget_next, ACTION_WIDGET_NEXT)
    }
}

@Composable
private fun IconBtn(
    context: Context, w: androidx.compose.ui.unit.Dp, icon: Int, action: String
) {
    Box(
        modifier = GlanceModifier.width(w).height(42.dp)
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
