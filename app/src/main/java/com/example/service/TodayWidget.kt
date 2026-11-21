package com.example.service

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.domain.DayCompletion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Home-screen widget: today's routine percent and the current streak. */
class TodayWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { TodayWidget.refresh(context.applicationContext) } finally { pending.finish() }
        }
    }
}

object TodayWidget {
    /** Redraws every placed widget from the database. Called after a tick, a day change and the system's own updates (including after a restart). */
    suspend fun refresh(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, TodayWidgetProvider::class.java))
        if (ids.isEmpty()) return
        val dao = AppDatabase.getDatabase(context).syncWellDao()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val items = dao.getDailyRoutineDirectly(today)?.let { dao.getDailyRoutineItems(it.id) } ?: emptyList()
        val done = items.count { it.isDone }
        val percent = Math.round(DayCompletion.of(done, items.size).percent * 100)
        val streak = dao.getDailyAnalyticsDirectly(today)?.streakValue ?: 0
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val views = RemoteViews(context.packageName, R.layout.widget_today).apply {
            setTextViewText(R.id.widget_percent, "$percent%")
            setTextViewText(R.id.widget_detail, if (items.isEmpty()) "Nothing scheduled" else "$done of ${items.size} done")
            setTextViewText(R.id.widget_streak, if (streak > 0) "$streak-day streak" else "Start a streak")
            setOnClickPendingIntent(R.id.widget_root, open)
        }
        manager.updateAppWidget(ids, views)
    }
}
