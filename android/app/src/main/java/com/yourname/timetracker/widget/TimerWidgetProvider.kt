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
import android.widget.Toast
import com.yourname.timetracker2.R

class TimerWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_START  = "com.yourname.timetracker2.widget.START"
        const val ACTION_PAUSE  = "com.yourname.timetracker2.widget.PAUSE"
        const val ACTION_RESUME = "com.yourname.timetracker2.widget.RESUME"
        const val ACTION_STOP   = "com.yourname.timetracker2.widget.STOP"
        const val ACTION_IGNORE = "com.yourname.timetracker2.widget.IGNORE"

        const val PREFS        = "timer_widget_prefs"
        const val KEY_STATE    = "state"          // idle / running / paused
        const val KEY_START    = "start_at"
        const val KEY_PAUSED   = "paused_total"
        const val KEY_PAUSED_AT= "paused_at"
        const val KEY_BASE     = "chrono_base"

        // Capacitor 的 Preferences 插件用的就是这个 SharedPreferences
        const val CAP_PREFS    = "CapacitorStorage"
        const val CAP_KEY      = "pendingTimer"
    }

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(ctx, mgr, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
    }

    fun updateWidgetPublic(context: Context, mgr: AppWidgetManager, id: Int) {
        val p     = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val state = p.getString(KEY_STATE, "idle")
        val v     = RemoteViews(context.packageName, R.layout.widget_timer)

        // Explicitly consume clicks on every non-button region. Do not rely on parent click propagation.
        val ignore = pi(context, ACTION_IGNORE)
        v.setOnClickPendingIntent(R.id.widget_root, ignore)
        v.setOnClickPendingIntent(R.id.chronometer, ignore)
        v.setOnClickPendingIntent(R.id.idle_text, ignore)
        v.setOnClickPendingIntent(R.id.widget_button_row, ignore)

        v.setOnClickPendingIntent(R.id.btn_start, pi(context, ACTION_START))
        v.setOnClickPendingIntent(R.id.btn_stop,  pi(context, ACTION_STOP))

        when (state) {
            "idle" -> {
                v.setViewVisibility(R.id.chronometer, View.GONE)
                v.setViewVisibility(R.id.idle_text,   View.VISIBLE)
                v.setTextViewText(R.id.idle_text, "00:00:00")
                v.setViewVisibility(R.id.btn_start,   View.VISIBLE)
                v.setViewVisibility(R.id.btn_pause,   View.GONE)
                v.setViewVisibility(R.id.btn_stop,    View.GONE)
            }
            "running" -> {
                v.setViewVisibility(R.id.chronometer, View.VISIBLE)
                v.setViewVisibility(R.id.idle_text,   View.GONE)
                v.setViewVisibility(R.id.btn_start,   View.GONE)
                v.setViewVisibility(R.id.btn_pause,   View.VISIBLE)
                v.setViewVisibility(R.id.btn_stop,    View.VISIBLE)
                v.setTextViewText(R.id.btn_pause, "暂停")
                v.setOnClickPendingIntent(R.id.btn_pause, pi(context, ACTION_PAUSE))

                val base = p.getLong(KEY_BASE, SystemClock.elapsedRealtime())
                v.setChronometer(R.id.chronometer, base, null, true)
            }
            "paused" -> {
                val startedAt = p.getLong(KEY_START, 0L)
                val pausedTot = p.getLong(KEY_PAUSED, 0L) +
                        (System.currentTimeMillis() - p.getLong(KEY_PAUSED_AT, 0L))
                val elapsed   = (System.currentTimeMillis() - startedAt - pausedTot).coerceAtLeast(0)

                v.setViewVisibility(R.id.chronometer, View.GONE)
                v.setViewVisibility(R.id.idle_text,   View.VISIBLE)
                v.setTextViewText(R.id.idle_text, fmt(elapsed))
                v.setViewVisibility(R.id.btn_start,   View.GONE)
                v.setViewVisibility(R.id.btn_pause,   View.VISIBLE)
                v.setViewVisibility(R.id.btn_stop,    View.VISIBLE)
                v.setTextViewText(R.id.btn_pause, "继续")
                v.setOnClickPendingIntent(R.id.btn_pause, pi(context, ACTION_RESUME))
            }
        }
        mgr.updateAppWidget(id, v)
    }

    private fun pi(ctx: Context, action: String): PendingIntent {
        val i = Intent(ctx, TimerWidgetActionReceiver::class.java).apply {
            this.action = action
            this.setPackage(ctx.packageName)
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }
        return PendingIntent.getBroadcast(
            ctx, action.hashCode(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun fmt(ms: Long): String {
        val s = ms / 1000
        return "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
    }
}
