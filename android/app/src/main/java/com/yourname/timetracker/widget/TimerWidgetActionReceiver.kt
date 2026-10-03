package com.yourname.timetracker2.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock

class TimerWidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in setOf(
                TimerWidgetProvider.ACTION_START,
                TimerWidgetProvider.ACTION_PAUSE,
                TimerWidgetProvider.ACTION_RESUME,
                TimerWidgetProvider.ACTION_STOP
            )) return

        val p = context.getSharedPreferences(TimerWidgetProvider.PREFS, Context.MODE_PRIVATE)
        val state = p.getString(TimerWidgetProvider.KEY_STATE, "idle")

        when (action) {
            TimerWidgetProvider.ACTION_START -> {
                p.edit()
                    .putString(TimerWidgetProvider.KEY_STATE, "running")
                    .putLong(TimerWidgetProvider.KEY_START, System.currentTimeMillis())
                    .putLong(TimerWidgetProvider.KEY_PAUSED, 0L)
                    .putLong(TimerWidgetProvider.KEY_PAUSED_AT, 0L)
                    .putLong(TimerWidgetProvider.KEY_BASE, SystemClock.elapsedRealtime())
                    .apply()
            }
            TimerWidgetProvider.ACTION_PAUSE -> if (state == "running") {
                p.edit()
                    .putString(TimerWidgetProvider.KEY_STATE, "paused")
                    .putLong(TimerWidgetProvider.KEY_PAUSED_AT, System.currentTimeMillis())
                    .apply()
            }
            TimerWidgetProvider.ACTION_RESUME -> if (state == "paused") {
                val pausedAt = p.getLong(TimerWidgetProvider.KEY_PAUSED_AT, 0L)
                val pauseDur = System.currentTimeMillis() - pausedAt
                val newPaused = p.getLong(TimerWidgetProvider.KEY_PAUSED, 0L) + pauseDur
                p.edit()
                    .putString(TimerWidgetProvider.KEY_STATE, "running")
                    .putLong(TimerWidgetProvider.KEY_PAUSED, newPaused)
                    .putLong(TimerWidgetProvider.KEY_PAUSED_AT, 0L)
                    .apply()
            }
            TimerWidgetProvider.ACTION_STOP -> if (state == "running" || state == "paused") {
                val startedAt = p.getLong(TimerWidgetProvider.KEY_START, 0L)
                var pausedTot = p.getLong(TimerWidgetProvider.KEY_PAUSED, 0L)
                if (state == "paused") {
                    pausedTot += System.currentTimeMillis() -
                        p.getLong(TimerWidgetProvider.KEY_PAUSED_AT, 0L)
                }
                val endTs = System.currentTimeMillis()
                val duration = (endTs - startedAt - pausedTot).coerceAtLeast(1000L)
                val json = """{"start":$startedAt,"end":$endTs,"duration":$duration,"pausedMs":$pausedTot}"""
                context.getSharedPreferences(TimerWidgetProvider.CAP_PREFS, Context.MODE_PRIVATE)
                    .edit().putString(TimerWidgetProvider.CAP_KEY, json).apply()

                p.edit()
                    .putString(TimerWidgetProvider.KEY_STATE, "idle")
                    .putLong(TimerWidgetProvider.KEY_START, 0L)
                    .putLong(TimerWidgetProvider.KEY_PAUSED, 0L)
                    .putLong(TimerWidgetProvider.KEY_PAUSED_AT, 0L)
                    .apply()
            }
        }

        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(
            ComponentName(context, TimerWidgetProvider::class.java)
        )
        ids.forEach { TimerWidgetProvider.updateWidgetPublic(context, mgr, it) }
    }
}
