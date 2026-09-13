package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DailyBackupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("SyncWell", "Daily 9PM backup triggered")
        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val dao = AppDatabase.getDatabase(context).syncWellDao()
                val settings = dao.getAppSettingsDirectly()
                
                if (settings != null && settings.backupEnabled && !settings.firebaseUid.isNullOrBlank()) {
                    Log.d("SyncWell", "Executing scheduled backup for ${settings.firebaseUid}")
                    val result = CloudBackupService.performBackup(context, dao, settings.firebaseUid)
                    result.fold(
                        onSuccess = { summary ->
                            Log.d("SyncWell", "Scheduled daily backup successful: ${summary.workoutSessionsCount} workouts, ${summary.todosCount} todos, ${summary.trackerSessionsCount} tracker sessions")
                        },
                        onFailure = { e ->
                            Log.e("SyncWell", "Scheduled daily backup failed", e)
                        }
                    )
                } else {
                    Log.d("SyncWell", "Scheduled backup skipped: backupEnabled=${settings?.backupEnabled}, uid=${settings?.firebaseUid}")
                }
            } catch (e: Exception) {
                Log.e("SyncWell", "Error in DailyBackupReceiver", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
