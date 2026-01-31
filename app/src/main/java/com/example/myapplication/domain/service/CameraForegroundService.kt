package com.example.myapplication.domain.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.myapplication.MainActivity
import com.example.myapplication.R

/**
 * 前台服务
 * 确保录像功能在后台运行时不被系统杀死
 */
class CameraForegroundService : Service() {

    companion object {
        private const val TAG = "CameraForegroundService"
        private const val CHANNEL_ID = "accessible_camera_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START_FOREGROUND = "START_FOREGROUND"
        const val ACTION_STOP_FOREGROUND = "STOP_FOREGROUND"
        const val ACTION_UPDATE_STATUS = "UPDATE_STATUS"

        const val EXTRA_STATUS_TEXT = "status_text"
    }

    private var statusText: String = "录像运行中"

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "前台服务已创建")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FOREGROUND -> {
                Log.d(TAG, "启动前台服务")
                statusText = intent.getStringExtra(EXTRA_STATUS_TEXT) ?: "录像运行中"
                startForeground(NOTIFICATION_ID, createNotification())
            }
            ACTION_UPDATE_STATUS -> {
                Log.d(TAG, "更新前台服务状态")
                statusText = intent.getStringExtra(EXTRA_STATUS_TEXT) ?: statusText
                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, createNotification())
            }
            ACTION_STOP_FOREGROUND -> {
                Log.d(TAG, "停止前台服务")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        // 如果服务被系统杀死后重启，保持前台状态
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * 创建通知渠道（Android 8.0+需要）
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "无障碍相机服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持录像功能在后台运行"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "通知渠道已创建")
        }
    }

    /**
     * 创建通知
     */
    private fun createNotification(): Notification {
        // 点击通知时返回主界面
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("无障碍相机")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_menu_camera) // 使用系统相机图标
            .setContentIntent(pendingIntent)
            .setOngoing(true) // 不可被用户滑动删除
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "前台服务已销毁")
    }
}
