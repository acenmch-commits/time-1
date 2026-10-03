package com.yourname.timetracker2.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.view.View
import android.os.SystemClock
import com.yourname.timetracker2.R

class TimerWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_START = "com.yourname.timetracker2.widget.START"
        const val ACTION_PAUSE = "com.yourname.timetracker2.widget.PAUSE"
        const val ACTION_RESUME = "com.yourname.timetracker2.widget.RESUME"
        const val ACTION_STOP = "com.yourname.timetracker2.widget.STOP"

        const val PREFS = "timer_widget_prefs"
        const val KEY_STATE = "state"
        const val KEY_START = "start_at"
        const val KEY_PAUSED = "paused_total"
        const val KEY_PAUSED_AT = "paused_at"
        const val KEY_BASE = "chrono_base"

        const val CAP_PREFS = "CapacitorStorage"
        const val CAP_KEY = "pendingTimer"
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidgetPublic(context, manager, it) }
    }

    fun updateWidgetPublic(context: Context, manager: AppWidgetManager, id: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val state = prefs.getString(KEY_STATE, "idle")
        val views = RemoteViews(context.packageName, R.layout.widget_timer)

        // IMPORTANT: no PendingIntent is assigned to the root or any non-button view.
        // Only the visible action buttons receive click handlers.
        views.setOnClickPendingIntent(R.id.btn_start, createActionPendingIntent(context, ACTION_START))
        views.setOnClickPendingIntent(R.id.btn_pause, createActionPendingIntent(context, ACTION_PAUSE))
        views.setOnClickPendingIntent(R.id.btn_stop, createActionPendingIntent(context, ACTION_STOP))

        when (state) {
            "running" -> {
                views.setViewVisibility(R.id.chronometer, View.VISIBLE)
                views.setViewVisibility(R.id.idle_text, View.GONE)
                views.setViewVisibility(R.id.btn_start, View.GONE)
                views.setViewVisibility(R.id.btn_pause, View.VISIBLE)
                views.setViewVisibility(R.id.btn_stop, View.VISIBLE)
                views.setTextViewText(R.id.btn_pause, "暂停")

                val base = prefs.getLong(KEY_BASE, SystemClock.elapsedRealtime())
                views.setChronometer(R.id.chronometer, base, null, true)
            }
            "paused" -> {
                val startedAt = prefs.getLong(KEY_START, 0L)
                val pausedAt = prefs.getLong(KEY_PAUSED_AT, 0L)
                val pausedTotal = prefs.getLong(KEY_PAUSED, 0L) +
                    (System.currentTimeMillis() - pausedAt).coerceAtLeast(0L)
                val elapsed = (System.currentTimeMillis() - startedAt - pausedTotal).coerceAtLeast(0L)

                views.setViewVisibility(R.id.chronometer, View.GONE)
                views.setViewVisibility(R.id.idle_text, View.VISIBLE)
                views.setTextViewText(R.id.idle_text, formatElapsed(elapsed))
                views.setViewVisibility(R.id.btn_start, View.GONE)
                views.setViewVisibility(R.id.btn_pause, View.VISIBLE)
                views.setViewVisibility(R.id.btn_stop, View.VISIBLE)
                views.setTextViewText(R.id.btn_pause, "继续")
                views.setOnClickPendingIntent(R.id.btn_pause, createActionPendingIntent(context, ACTION_RESUME))
            }
            else -> {
                views.setViewVisibility(R.id.chronometer, View.GONE)
                views.setViewVisibility(R.id.idle_text, View.VISIBLE)
                views.setTextViewText(R.id.idle_text, "00:00:00")
                views.setViewVisibility(R.id.btn_start, View.VISIBLE)
                views.setViewVisibility(R.id.btn_pause, View.GONE)
                views.setViewVisibility(R.id.btn_stop, View.GONE)
            }
        }

        manager.updateAppWidget(id, views)
    }

    private fun createActionPendingIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, TimerWidgetActionReceiver::class.java).apply {
            this.action = action
            setPackage(context.packageName)
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
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
