package com.example.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import java.util.*

class FocusTimerService : Service() {

    companion object {
        const val CHANNEL_ID = "focus_timer"
        const val ATTENTION_CHANNEL_ID = "focus_attention_check"
        const val NOTIFICATION_ID = 1001
        const val ATTENTION_NOTIFICATION_ID = 1002
        const val PAUSE_SENTINEL_NOTIFICATION_ID = 1003

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_STILL_HERE = "ACTION_STILL_HERE"
        const val ACTION_PAUSE = "ACTION_PAUSE"

        const val EXTRA_CATEGORY = "EXTRA_CATEGORY"
        const val EXTRA_TAG = "EXTRA_TAG"
        const val EXTRA_START_TIME = "EXTRA_START_TIME"

        const val ACTION_SCHEDULE_CHECK = "ACTION_SCHEDULE_CHECK"
        const val ACTION_CANCEL_CHECK = "ACTION_CANCEL_CHECK"
    }

    private var category: String? = null
    private var taskTag: String? = null
    private var startTimeMs: Long = 0

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var checkRunnable: Runnable? = null
    private var autoPauseRunnable: Runnable? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                category = intent.getStringExtra(EXTRA_CATEGORY)
                taskTag = intent.getStringExtra(EXTRA_TAG)
                startTimeMs = intent.getLongExtra(EXTRA_START_TIME, System.currentTimeMillis())
                startForeground(NOTIFICATION_ID, buildLiveNotification())
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_STILL_HERE -> {
                cancelAttentionCheck()
            }
            ACTION_PAUSE -> {
                val stopIntent = Intent("com.example.FOCUS_PAUSE")
                sendBroadcast(stopIntent)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_SCHEDULE_CHECK -> {
                scheduleAttentionFlow()
            }
            ACTION_CANCEL_CHECK -> {
                cancelAttentionCheck()
            }
        }
        return START_NOT_STICKY
    }

    private fun scheduleAttentionFlow() {
        cancelAttentionCheck()

        // T+5:00 Notification
        checkRunnable = Runnable { postCheckNotification() }
        handler.postDelayed(checkRunnable!!, 300_000)

        // T+6:00 Auto-Pause
        autoPauseRunnable = Runnable {
            postPauseSentinel()
            val stopIntent = Intent("com.example.FOCUS_PAUSE")
            sendBroadcast(stopIntent)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        handler.postDelayed(autoPauseRunnable!!, 360_000)
    }

    private fun postCheckNotification() {
        val stillHereIntent = Intent(this, FocusTimerService::class.java).apply { action = ACTION_STILL_HERE }
        val stillHerePending = PendingIntent.getService(this, 1, stillHereIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val pauseIntent = Intent(this, FocusTimerService::class.java).apply { action = ACTION_PAUSE }
        val pausePending = PendingIntent.getService(this, 2, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val label = when (category) {
            "STUDY" -> "📚 Study"
            "WORK" -> "💼 Work"
            else -> category ?: "Focus"
        }

        val notification = NotificationCompat.Builder(this, ATTENTION_CHANNEL_ID)
            .setContentTitle("Still on $label?")
            .setContentText("SyncWell noticed you stepped away. Confirm or the session will pause.")
            .setSmallIcon(R.drawable.icon)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setColor(0xFFF59E0B.toInt())
            .addAction(0, "✅ Still Studying", stillHerePending)
            .addAction(0, "⏸ Pause Session", pausePending)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(ATTENTION_NOTIFICATION_ID, notification)
    }

    private fun postPauseSentinel() {
        val label = when (category) {
            "STUDY" -> "Study"
            "WORK" -> "Work"
            else -> category ?: "Focus"
        }
        val notification = NotificationCompat.Builder(this, ATTENTION_CHANNEL_ID)
            .setContentTitle("Session Paused")
            .setContentText("$label session was automatically paused after no response.")
            .setSmallIcon(R.drawable.icon)
            .setColor(0xFFEF4444.toInt())
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(PAUSE_SENTINEL_NOTIFICATION_ID, notification)
    }

    private fun buildLiveNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val label = when (category) {
            "STUDY" -> "📚 Study Session"
            "WORK" -> "💼 Work Session"
            "TIMEPASS" -> "🎮 Leisure Session"
            "SPORT" -> "🏸 Sport Session"
            "SLEEP" -> "😴 Sleep Tracking"
            else -> category ?: "Focus Session"
        }
        val tagLine = if (taskTag != null) " · $taskTag" else ""

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("$label$tagLine")
            .setContentText("⏱ Timing active — Tap to return to SyncWell")
            .setSmallIcon(R.drawable.icon)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(true)
            .setWhen(startTimeMs)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .setColor(0xFF10B981.toInt())
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // Live Timer Channel (High priority for ColorOS 15 pill)
            val timerChannel = NotificationChannel(
                CHANNEL_ID, "Focus Timer",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows live running clock in status bar"
                setSound(null, null)
                enableLights(false)
                setShowBadge(true)
            }
            manager.createNotificationChannel(timerChannel)

            // Attention Check Channel
            val attentionChannel = NotificationChannel(
                ATTENTION_CHANNEL_ID, "Attention Checks",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminds you to check in if you step away"
            }
            manager.createNotificationChannel(attentionChannel)
        }
    }

    private fun cancelAttentionCheck() {
        checkRunnable?.let { handler.removeCallbacks(it) }
        autoPauseRunnable?.let { handler.removeCallbacks(it) }

        val manager = getSystemService(NotificationManager::class.java)
        manager.cancel(ATTENTION_NOTIFICATION_ID)
        manager.cancel(PAUSE_SENTINEL_NOTIFICATION_ID)
    }
}
