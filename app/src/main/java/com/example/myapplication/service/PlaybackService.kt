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

//MainActivity 创建 MediaController，尝试连接PlaybackService
//系统发现服务还没有创建，于是调用：PlaybackService.onCreate()
//在 onCreate() 中首先创建 ExoPlayer：可以理解为：PlaybackService 启动 创建一个长期存在的 ExoPlayer（准备接收播放命令）

//用户按Home键之后，MainActivity 进入后台，Compose 页面不再显示
//但是音乐不会停止，因为播放器不在 MainActivity 中，而在PlaybackService

class PlaybackService : MediaSessionService() {//继承MediaSessionService，

    //PlaybackService：保证播放器可以脱离页面继续运行
    //MediaSession：连接应用、通知栏、锁屏和蓝牙设备
    //ExoPlayer：真正加载和播放音乐

    private var mediaSession: MediaSession? = null
    //MediaSession 本身通常不负责真正播放声音，真正播放的是 ExoPlayer。MediaSession 主要负责把各种控制入口统一起来
    //比如点击暂停、下一首，都是通过mediaSession控制ExoPlayer
    //MediaSession 最核心的价值就是：让后台 ExoPlayer 可以被应用 UI、系统通知栏、锁屏和蓝牙设备统一控制。

    override fun onCreate() {//Step 1 — Service 创建时初始化播放器和会话
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()//配置音频
            .setUsage(C.USAGE_MEDIA)//音频用途：媒体播放
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)//音频内容：音乐
            .build()

        val player = ExoPlayer.Builder(this)//ExoPlayer负责播放音乐，
            // 这里this表示当前的 PlaybackService，也就是使用 Service 作为播放器的 Context
            .build()
            .apply {
                // 自动申请和释放音频焦点
                setAudioAttributes(
                    audioAttributes,//参数1：告诉 ExoPlayer 当前播放的是媒体音乐
                    true//参数2：表示让 ExoPlayer自动处理音频焦点。
                    //ps:当前应用中可能有多个应用播放声音，但是系统需要决定当前哪个应用可以主要播放声音，这就是音频焦点
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

        val sessionActivity = PendingIntent.getActivity(//PendingIntent先把这个页面跳转操作交给 Android 系统，等用户以后点击通知时再执行。
            this,
            1001,
            openPlayerIntent,//点击通知后执行实际页面跳转
            PendingIntent.FLAG_UPDATE_CURRENT or//如果之前已经存在相同的 PendingIntent，则使用新的 Intent 数据更新它。
                    PendingIntent.FLAG_IMMUTABLE//FLAG_IMMUTABLE创建后，外部不能修改这个 PendingIntent 的内容。
        )

        mediaSession = MediaSession.Builder(//MediaSession.Builder(this, player) 把 ExoPlayer 和 MediaSession 绑定在一起。
            // 此后所有通过 MediaSession 下发的播放命令（播放、暂停、切歌等），最终都会由 ExoPlayer 执行。
            this,//当前的PlaybackService
            player//刚创建的ExoPlayer
        )
            .setSessionActivity(sessionActivity)//系统媒体通知被点击后会执行sessionActivity 打开 MainActivity 携带 ACTION_OPEN_PLAYER
            .build()
    }

    override fun onGetSession(//Step 2 — onGetSession() 交出会话

        //当客户端（MainActivity）请求连接时，Media3 框架调用 onGetSession()。
        //Service 把自己持有的 mediaSession 返回给框架，框架再把它"桥接"给客户端的 MediaController。
        //MainActivity 创建 SessionToken
        //    ↓
        //MediaController 请求连接 PlaybackService
        //    ↓
        //PlaybackService.onGetSession()
        //    ↓
        //返回 mediaSession
        //    ↓
        //MediaController 连接成功
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {//当 PlaybackService 被真正销毁时，系统会调用这个方法
        mediaSession?.run {
            player.release()
            release()
        }

        mediaSession = null
        super.onDestroy()
    }
}