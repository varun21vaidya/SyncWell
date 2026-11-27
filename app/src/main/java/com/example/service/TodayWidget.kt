package com.example.service

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.SizeF
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.JourneyState
import com.example.domain.DayCompletion
import com.example.domain.JourneyCharacters
import com.example.domain.JourneyMap
import com.example.domain.RegionDef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Home-screen widget: today's routine percent, the streak and, when the Journey is on, where the character stands. */
class TodayWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { TodayWidget.refresh(context.applicationContext) } finally { pending.finish() }
        }
    }
}

object TodayWidget {
    private class Today(val percent: Long, val detail: String, val streak: String)

    /** Redraws every placed widget from the database. Called after a tick, a day change and the system's own updates (including after a restart). */
    suspend fun refresh(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, TodayWidgetProvider::class.java))
        if (ids.isEmpty()) return
        val dao = AppDatabase.getDatabase(context).syncWellDao()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val items = dao.getDailyRoutineDirectly(today)?.let { dao.getDailyRoutineItems(it.id) } ?: emptyList()
        val done = items.count { it.isDone }
        val streak = dao.getDailyAnalyticsDirectly(today)?.streakValue ?: 0
        val numbers = Today(
            Math.round(DayCompletion.of(done, items.size).percent * 100),
            if (items.isEmpty()) "Nothing scheduled" else "$done of ${items.size} done",
            if (streak > 0) "$streak-day streak" else "Start a streak"
        )
        val journey = dao.getJourneyState()?.takeIf { it.enabled }
        val regions = if (journey != null) loadRegions(context) else emptyList()
        val compact = bind(context, RemoteViews(context.packageName, R.layout.widget_today), numbers, journey, regions)
        // Android 12+ picks the wide layout, with the map, once the widget is resized wide enough; older versions keep the compact one
        val views = if (Build.VERSION.SDK_INT >= 31 && journey != null) {
            RemoteViews(mapOf(
                SizeF(110f, 110f) to compact,
                SizeF(250f, 110f) to bind(context, RemoteViews(context.packageName, R.layout.widget_today_wide), numbers, journey, regions)
            ))
        } else compact
        manager.updateAppWidget(ids, views)
    }

    private fun bind(context: Context, views: RemoteViews, n: Today, journey: JourneyState?, regions: List<RegionDef>): RemoteViews = views.apply {
        setTextViewText(R.id.widget_percent, "${n.percent}%")
        setTextViewText(R.id.widget_detail, n.detail)
        setTextViewText(R.id.widget_streak, n.streak)
        setOnClickPendingIntent(R.id.widget_root, open(context, 0, null))
        setOnClickPendingIntent(R.id.widget_tick, open(context, 2, "ROUTINE")) // a tick needs the app: its linked goals and protein question run there
        if (journey == null) return@apply
        setViewVisibility(R.id.widget_journey, android.view.View.VISIBLE)
        setTextViewText(R.id.widget_journey, JourneyMap.stripText(journey, regions))
        setOnClickPendingIntent(R.id.widget_journey, open(context, 1, "JOURNEY"))
        regions.getOrNull(journey.regionIndex)?.let { region ->
            runCatching {
                setImageViewBitmap(R.id.widget_map, JourneyWidgetArt.render(context, region, journey.stepInRegion, JourneyCharacters.byId(journey.characterId)))
                setOnClickPendingIntent(R.id.widget_map, open(context, 1, "JOURNEY"))
            } // the compact layout has no map view, which is fine
        }
    }

    private fun open(context: Context, request: Int, destination: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply { destination?.let { putExtra(MainActivity.EXTRA_DESTINATION, it) } }
        return PendingIntent.getActivity(context, request, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun loadRegions(context: Context) =
        JourneyMap.parse(context.resources.openRawResource(R.raw.adventure_route).bufferedReader().use { it.readText() })
}
