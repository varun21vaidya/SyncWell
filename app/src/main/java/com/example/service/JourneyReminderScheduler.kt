package com.example.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.domain.JourneyReminderInput
import com.example.domain.JourneyReminderPolicy
import com.example.domain.JourneyReminderSettings
import com.example.domain.JourneySlot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Fires one of the Journey's five daily slots. */
class JourneyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val slot = intent.getStringExtra("slot")?.let { runCatching { JourneySlot.valueOf(it) }.getOrNull() } ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                JourneyReminderScheduler.onAlarm(context.applicationContext, slot)
            } catch (e: Exception) {
                Log.e("SyncWell", "Journey reminder failed", e)
            } finally {
                pending.finish()
            }
        }
    }
}

/** Alarms, settings and posting for the Journey's five reminders. The decision itself is [JourneyReminderPolicy]. */
object JourneyReminderScheduler {
    private const val PREFS = "syncwell_session_prefs"
    private const val CHANNEL = "journey_reminders"
    private const val REQ_BASE = 9200
    private const val NOTIFICATION_ID = 2100

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun alarms(context: Context) = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private fun intent(context: Context, slot: JourneySlot) = Intent(context, JourneyReminderReceiver::class.java).putExtra("slot", slot.name)
    private fun pending(context: Context, slot: JourneySlot) =
        PendingIntent.getBroadcast(context, REQ_BASE + slot.ordinal, intent(context, slot), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun settings(context: Context): JourneyReminderSettings {
        val p = prefs(context)
        val on = p.getStringSet("jrem_slots", null)?.mapNotNull { runCatching { JourneySlot.valueOf(it) }.getOrNull() }?.toSet() ?: JourneySlot.values().toSet()
        return JourneyReminderSettings(
            master = p.getBoolean("jrem_master", false), slots = on,
            quietStart = p.getInt("jrem_quiet_start", -1).takeIf { it >= 0 }, quietEnd = p.getInt("jrem_quiet_end", -1).takeIf { it >= 0 }
        )
    }

    /** Stores the settings and re-arms the alarms (or cancels them all when the master switch is off). */
    suspend fun save(context: Context, s: JourneyReminderSettings) {
        prefs(context).edit().putBoolean("jrem_master", s.master).putStringSet("jrem_slots", s.slots.map { it.name }.toSet())
            .putInt("jrem_quiet_start", s.quietStart ?: -1).putInt("jrem_quiet_end", s.quietEnd ?: -1).apply()
        scheduleAll(context)
        ReminderScheduler.scheduleAll(context) // the two gentle ones come back when the five go off
    }

    /** Sets the next occurrence of every enabled slot and cancels the rest. Safe to call repeatedly. */
    suspend fun scheduleAll(context: Context) {
        val s = settings(context)
        val journeyOn = AppDatabase.getDatabase(context).syncWellDao().getJourneyState()?.enabled == true
        JourneySlot.values().forEach { slot ->
            if (s.master && journeyOn && slot in s.slots) schedule(context, slot) else alarms(context).cancel(pending(context, slot))
        }
    }

    private fun schedule(context: Context, slot: JourneySlot) {
        val at = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, slot.hour); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis() + 60_000) add(Calendar.DAY_OF_YEAR, 1)
        }
        // Inexact and allowed while idle, like the gentle reminders: no exact-alarm permission, a few minutes of drift is fine
        alarms(context).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.timeInMillis, pending(context, slot))
    }

    suspend fun onAlarm(context: Context, slot: JourneySlot) {
        scheduleAll(context) // the next occurrence, before anything can fail
        val dao = AppDatabase.getDatabase(context).syncWellDao()
        val appSettings = dao.getAppSettingsDirectly() ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val p = prefs(context)
        val sentToday = if (p.getString("jrem_sent_date", null) == today) (p.getStringSet("jrem_sent", null) ?: emptySet()).mapNotNull { runCatching { JourneySlot.valueOf(it) }.getOrNull() }.toSet() else emptySet()
        val (done, total) = RoutineProgress.counts(dao, today)
        val now = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
        val go = JourneyReminderPolicy.shouldPost(JourneyReminderInput(
            slot = slot, journeyEnabled = dao.getJourneyState()?.enabled == true, settings = settings(context),
            vacation = appSettings.vacationSince != null || dao.getVacationDayByDate(today) != null, nowMinutes = now,
            defaultQuietStart = minutesOf(appSettings.sleepTime, 23 * 60), defaultQuietEnd = minutesOf(appSettings.wakeTime, 7 * 60),
            done = done, total = total, sentToday = sentToday
        ))
        if (!go || !post(context, slot)) return // no permission: nothing counted as sent
        p.edit().putString("jrem_sent_date", today).putStringSet("jrem_sent", (sentToday + slot).map { it.name }.toSet()).apply()
    }

    private fun post(context: Context, slot: JourneySlot): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(CHANNEL, "Journey reminders", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_DESTINATION, "JOURNEY"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(
            NOTIFICATION_ID + slot.ordinal,
            NotificationCompat.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_notification).setContentTitle("SyncWell").setContentText(slot.body)
                .setContentIntent(open).setAutoCancel(true).build()
        )
        return true
    }

    private fun minutesOf(hhmm: String, fallback: Int) = JourneyReminderPolicy.parseTime(hhmm) ?: fallback
}
