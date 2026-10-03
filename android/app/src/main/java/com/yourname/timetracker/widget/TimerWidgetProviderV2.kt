package com.yourname.timetracker2.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.yourname.timetracker2.R

/**
 * Fresh widget provider with a new component identity.
 *
 * It intentionally has no click action on the root container. Only the
 * visible action buttons receive PendingIntents.
 */
class TimerWidgetProviderV2 : AppWidgetProvider() {

    companion object {
        private const val PREFS = TimerWidgetProvider.PREFS
        private const val KEY_STATE = TimerWidgetProvider.KEY_STATE
        private const val KEY_START = TimerWidgetProvider.KEY_START
        private const val KEY_PAUSED = TimerWidgetProvider.KEY_PAUSED
        private const val KEY_PAUSED_AT = TimerWidgetProvider.KEY_PAUSED_AT
        private const val KEY_BASE = TimerWidgetProvider.KEY_BASE

        private const val ACTION_START = TimerWidgetProvider.ACTION_START
        private const val ACTION_PAUSE = TimerWidgetProvider.ACTION_PAUSE
        private const val ACTION_RESUME = TimerWidgetProvider.ACTION_RESUME
        private const val ACTION_STOP = TimerWidgetProvider.ACTION_STOP
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidgetPublic(context, manager, it) }
    }

    fun updateWidgetPublic(context: Context, manager: AppWidgetManager, id: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val state = prefs.getString(KEY_STATE, "idle")
        val views = RemoteViews(context.packageName, R.layout.widget_timer_v2)

        views.setOnClickPendingIntent(
            R.id.btn_start_v2,
            createActionPendingIntent(context, ACTION_START, id, 1)
        )
        views.setOnClickPendingIntent(
            R.id.btn_pause_v2,
            createActionPendingIntent(context, ACTION_PAUSE, id, 2)
        )
        views.setOnClickPendingIntent(
            R.id.btn_stop_v2,
            createActionPendingIntent(context, ACTION_STOP, id, 4)
        )

        when (state) {
            "running" -> {
                views.setViewVisibility(R.id.chronometer_v2, View.VISIBLE)
                views.setViewVisibility(R.id.idle_text_v2, View.GONE)
                views.setViewVisibility(R.id.btn_start_v2, View.GONE)
                views.setViewVisibility(R.id.btn_pause_v2, View.VISIBLE)
                views.setViewVisibility(R.id.btn_stop_v2, View.VISIBLE)
                views.setTextViewText(R.id.btn_pause_v2, "暂停")
                val base = prefs.getLong(KEY_BASE, SystemClock.elapsedRealtime())
                views.setChronometer(R.id.chronometer_v2, base, null, true)
            }

            "paused" -> {
                val startedAt = prefs.getLong(KEY_START, 0L)
                val pausedAt = prefs.getLong(KEY_PAUSED_AT, 0L)
                val pausedTotal = prefs.getLong(KEY_PAUSED, 0L) +
                    (System.currentTimeMillis() - pausedAt).coerceAtLeast(0L)
                val elapsed = (System.currentTimeMillis() - startedAt - pausedTotal).coerceAtLeast(0L)

                views.setViewVisibility(R.id.chronometer_v2, View.GONE)
                views.setViewVisibility(R.id.idle_text_v2, View.VISIBLE)
                views.setTextViewText(R.id.idle_text_v2, formatElapsed(elapsed))
                views.setViewVisibility(R.id.btn_start_v2, View.GONE)
                views.setViewVisibility(R.id.btn_pause_v2, View.VISIBLE)
                views.setViewVisibility(R.id.btn_stop_v2, View.VISIBLE)
                views.setTextViewText(R.id.btn_pause_v2, "继续")
                views.setOnClickPendingIntent(
                    R.id.btn_pause_v2,
                    createActionPendingIntent(context, ACTION_RESUME, id, 3)
                )
            }

            else -> {
                views.setViewVisibility(R.id.chronometer_v2, View.GONE)
                views.setViewVisibility(R.id.idle_text_v2, View.VISIBLE)
                views.setTextViewText(R.id.idle_text_v2, "00:00:00")
                views.setViewVisibility(R.id.btn_start_v2, View.VISIBLE)
                views.setViewVisibility(R.id.btn_pause_v2, View.GONE)
                views.setViewVisibility(R.id.btn_stop_v2, View.GONE)
            }
        }

        manager.updateAppWidget(id, views)
    }

    private fun createActionPendingIntent(
        context: Context,
        action: String,
        widgetId: Int,
        actionCode: Int
    ): PendingIntent {
        val intent = Intent(context, TimerWidgetActionReceiver::class.java).apply {
            this.action = action
            setPackage(context.packageName)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }

        return PendingIntent.getBroadcast(
            context,
            2000 + widgetId * 10 + actionCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun formatElapsed(ms: Long): String {
        val totalSeconds = ms / 1000L
        return "%02d:%02d:%02d".format(
            totalSeconds / 3600L,
            (totalSeconds % 3600L) / 60L,
            totalSeconds % 60L
        )
    }
}
