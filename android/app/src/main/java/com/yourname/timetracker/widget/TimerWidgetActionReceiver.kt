package com.yourname.timetracker2.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock

class TimerWidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            TimerWidgetConstants.ACTION_START -> {
                val p = context.getSharedPreferences(TimerWidgetConstants.PREFS, Context.MODE_PRIVATE)
                p.edit()
                    .putString(TimerWidgetConstants.KEY_STATE, "running")
                    .putLong(TimerWidgetConstants.KEY_START, System.currentTimeMillis())
                    .putLong(TimerWidgetConstants.KEY_PAUSED, 0L)
                    .putLong(TimerWidgetConstants.KEY_PAUSED_AT, 0L)
                    .putLong(TimerWidgetConstants.KEY_BASE, SystemClock.elapsedRealtime())
                    .apply()
            }

            TimerWidgetConstants.ACTION_PAUSE -> {
                val p = context.getSharedPreferences(TimerWidgetConstants.PREFS, Context.MODE_PRIVATE)
                if (p.getString(TimerWidgetConstants.KEY_STATE, "idle") == "running") {
                    p.edit()
                        .putString(TimerWidgetConstants.KEY_STATE, "paused")
                        .putLong(TimerWidgetConstants.KEY_PAUSED_AT, System.currentTimeMillis())
                        .apply()
                }
            }

            TimerWidgetConstants.ACTION_RESUME -> {
                val p = context.getSharedPreferences(TimerWidgetConstants.PREFS, Context.MODE_PRIVATE)
                if (p.getString(TimerWidgetConstants.KEY_STATE, "idle") == "paused") {
                    val now = System.currentTimeMillis()
                    val pausedAt = p.getLong(TimerWidgetConstants.KEY_PAUSED_AT, now)
                    val pauseDur = (now - pausedAt).coerceAtLeast(0L)
                    p.edit()
                        .putString(TimerWidgetConstants.KEY_STATE, "running")
                        .putLong(
                            TimerWidgetConstants.KEY_PAUSED,
                            p.getLong(TimerWidgetConstants.KEY_PAUSED, 0L) + pauseDur
                        )
                        .putLong(TimerWidgetConstants.KEY_PAUSED_AT, 0L)
                        .putLong(
                            TimerWidgetConstants.KEY_BASE,
                            SystemClock.elapsedRealtime() -
                                (now - p.getLong(TimerWidgetConstants.KEY_START, now) -
                                    p.getLong(TimerWidgetConstants.KEY_PAUSED, 0L) - pauseDur)
                                    .coerceAtLeast(0L)
                        )
                        .apply()
                }
            }

            TimerWidgetConstants.ACTION_STOP -> {
                val p = context.getSharedPreferences(TimerWidgetConstants.PREFS, Context.MODE_PRIVATE)
                val state = p.getString(TimerWidgetConstants.KEY_STATE, "idle")

                if (state == "running" || state == "paused") {
                    val startedAt = p.getLong(TimerWidgetConstants.KEY_START, 0L)
                    var pausedTotal = p.getLong(TimerWidgetConstants.KEY_PAUSED, 0L)
                    if (state == "paused") {
                        pausedTotal += (
                            System.currentTimeMillis() -
                                p.getLong(TimerWidgetConstants.KEY_PAUSED_AT, 0L)
                            ).coerceAtLeast(0L)
                    }

                    val endTs = System.currentTimeMillis()
                    val duration = (endTs - startedAt - pausedTotal).coerceAtLeast(1000L)
                    val json =
                        """{"start":$startedAt,"end":$endTs,"duration":$duration,"pausedMs":$pausedTotal}"""

                    context.getSharedPreferences(
                        TimerWidgetConstants.CAP_PREFS,
                        Context.MODE_PRIVATE
                    ).edit().putString(
                        TimerWidgetConstants.CAP_KEY,
                        json
                    ).apply()

                    p.edit()
                        .putString(TimerWidgetConstants.KEY_STATE, "idle")
                        .putLong(TimerWidgetConstants.KEY_START, 0L)
                        .putLong(TimerWidgetConstants.KEY_PAUSED, 0L)
                        .putLong(TimerWidgetConstants.KEY_PAUSED_AT, 0L)
                        .apply()
                }
            }

            else -> return
        }

        refreshAll(context)
    }

    private fun refreshAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, TimerWidgetProviderV3::class.java)
        )
        ids.forEach { TimerWidgetProviderV3.updateWidget(context, manager, it) }
    }
}
