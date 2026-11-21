package com.example.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.RoutineSchedule
import com.example.domain.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Alarms for the gentle reminders and the 9 PM backup, plus the decision and posting when a reminder alarm fires. */
object ReminderScheduler {
    private const val PREFS = "syncwell_session_prefs"
    private const val CHANNEL = "gentle_reminders"
    private const val REQ_BACKUP = 9000
    private const val REQ_MORNING = 9101
    private const val REQ_EVENING = 9102
    private const val NOTIFICATION_ID = 2001

    fun isEnabled(context: Context) = prefs(context).getBoolean("reminders_enabled", false)

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun alarms(context: Context) = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun pending(context: Context, request: Int, intent: Intent) =
        PendingIntent.getBroadcast(context, request, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun slotIntent(context: Context, slot: ReminderSlot) =
        Intent(context, ReminderReceiver::class.java).putExtra("slot", slot.name)

    fun scheduleBackup9PM(context: Context) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
            if (timeInMillis < System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        alarms(context).setInexactRepeating(
            AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY,
            pending(context, REQ_BACKUP, Intent(context, DailyBackupReceiver::class.java))
        )
    }

    /** Sets the next morning and evening alarms, or cancels them when reminders are off. Safe to call repeatedly. */
    suspend fun scheduleAll(context: Context) {
        val settings = AppDatabase.getDatabase(context).syncWellDao().getAppSettingsDirectly()
        val slots = mapOf(ReminderSlot.MORNING to REQ_MORNING, ReminderSlot.EVENING to REQ_EVENING)
        if (settings == null || !isEnabled(context)) {
            slots.forEach { (slot, req) -> alarms(context).cancel(pending(context, req, slotIntent(context, slot))) }
            return
        }
        val wake = minutesOf(settings.wakeTime, 7 * 60)
        val sleep = minutesOf(settings.sleepTime, 23 * 60)
        scheduleSlot(context, ReminderSlot.MORNING, REQ_MORNING, (wake + 15) % 1440)
        scheduleSlot(context, ReminderSlot.EVENING, REQ_EVENING, (sleep - 180 + 1440) % 1440)
    }

    private fun scheduleSlot(context: Context, slot: ReminderSlot, request: Int, minutes: Int) {
        val at = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minutes / 60); set(Calendar.MINUTE, minutes % 60); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis() + 60_000) add(Calendar.DAY_OF_YEAR, 1)
        }
        // Inexact and allowed while idle: no exact-alarm permission needed, a few minutes of drift is fine for a nudge
        alarms(context).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.timeInMillis, pending(context, request, slotIntent(context, slot)))
    }

    /** Runs when a reminder alarm fires: chains the next alarm first, then decides and posts at most one reminder. */
    suspend fun onAlarm(context: Context, slot: ReminderSlot) {
        scheduleAll(context) // the next occurrence, before anything can fail
        if (!isEnabled(context)) return
        val dao = AppDatabase.getDatabase(context).syncWellDao()
        val settings = dao.getAppSettingsDirectly() ?: return
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = sdf.format(Date())

        val routine = dao.getDailyRoutineDirectly(today)
        val items = routine?.let { dao.getDailyRoutineItems(it.id) } ?: emptyList()
        val done = items.count { it.isDone }
        val total = if (items.isNotEmpty()) items.size
        else RoutineSchedule.templatesForDate(dao.getRoutineTemplates(), today).sumOf { dao.getRoutineTemplateItems(it.id).size } // app not opened yet today

        val history = dao.getAllDailyAnalytics()
        val streak = history.firstOrNull { it.dateKey == today }?.streakValue
            ?: history.firstOrNull { it.dateKey == WeekKey.addDays(today, -1) }?.streakValue ?: 0
        val lastActive = history.filter { it.routineCompletionPct > 0 }.maxOfOrNull { it.dateKey }
        val idleDays = lastActive?.let { WeekKey.range(it, today).size - 1 } ?: 0

        val p = prefs(context)
        val sentToday = if (p.getString("rem_sent_date", null) == today) (p.getStringSet("rem_sent_types", null) ?: emptySet()).map { ReminderType.valueOf(it) }.toSet() else emptySet()
        if (p.getString("rem_lapse_since", null) != (lastActive ?: "")) { // the user was active again: a new break starts counting from zero
            p.edit().putString("rem_lapse_since", lastActive ?: "").putInt("rem_comeback_count", 0).apply()
        }
        val now = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }

        val type = ReminderPolicy.decide(ReminderInput(
            slot = slot, enabled = true,
            vacation = settings.vacationSince != null || dao.getVacationDayByDate(today) != null,
            nowMinutes = now, quietStartMinutes = minutesOf(settings.sleepTime, 23 * 60), quietEndMinutes = minutesOf(settings.wakeTime, 7 * 60),
            done = done, total = total, streak = streak, idleDays = idleDays,
            comebackSent = p.getInt("rem_comeback_count", 0), sentToday = sentToday
        )) ?: return
        if (!post(context, type, total, DayCompletion.itemsToQualify(done, total), streak)) return // no permission: nothing counted as sent
        val editor = p.edit().putString("rem_sent_date", today).putStringSet("rem_sent_types", (sentToday + type).map { it.name }.toSet())
        if (type == ReminderType.COMEBACK) editor.putInt("rem_comeback_count", p.getInt("rem_comeback_count", 0) + 1)
        editor.apply()
    }

    private fun post(context: Context, type: ReminderType, total: Int, itemsToGo: Int, streak: Int): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(CHANNEL, "Gentle reminders", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(
            NOTIFICATION_ID + type.ordinal,
            NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(ReminderCopy.title(type))
                .setContentText(ReminderCopy.body(type, total, itemsToGo, streak))
                .setContentIntent(open).setAutoCancel(true).build()
        )
        return true
    }

    private fun minutesOf(hhmm: String, fallback: Int): Int {
        val p = hhmm.split(":")
        val h = p.getOrNull(0)?.toIntOrNull() ?: return fallback
        val m = p.getOrNull(1)?.toIntOrNull() ?: return fallback
        return h * 60 + m
    }
}
