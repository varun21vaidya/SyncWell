package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.domain.ReminderSlot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fires for a morning or evening reminder slot. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val slot = intent.getStringExtra("slot")?.let { runCatching { ReminderSlot.valueOf(it) }.getOrNull() } ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ReminderScheduler.onAlarm(context.applicationContext, slot)
            } catch (e: Exception) {
                Log.e("SyncWell", "Reminder failed", e)
            } finally {
                pending.finish()
            }
        }
    }
}

/** Alarms are cleared by a restart, so put the reminders and the 9 PM backup back. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ReminderScheduler.scheduleBackup9PM(context.applicationContext)
                ReminderScheduler.scheduleAll(context.applicationContext)
            } catch (e: Exception) {
                Log.e("SyncWell", "Reschedule after boot failed", e)
            } finally {
                pending.finish()
            }
        }
    }
}
