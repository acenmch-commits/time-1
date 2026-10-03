package com.yourname.timetracker2.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock

class TimerWidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            refreshAll(context)
            return
        }

        when (intent.action) {
            TimerWidgetProvider.ACTION_START -> {
                val p = context.getSharedPreferences(TimerWidgetProvider.PREFS, Context.MODE_PRIVATE)
                p.edit()
                    .putString(TimerWidgetProvider.KEY_STATE, "running")
                    .putLong(TimerWidgetProvider.KEY_START, System.currentTimeMillis())
                    .putLong(TimerWidgetProvider.KEY_PAUSED, 0L)
                    .putLong(TimerWidgetProvider.KEY_PAUSED_AT, 0L)
                    .putLong(TimerWidgetProvider.KEY_BASE, SystemClock.elapsedRealtime())
                    .apply()
            }

            TimerWidgetProvider.ACTION_PAUSE -> {
                val p = context.getSharedPreferences(TimerWidgetProvider.PREFS, Context.MODE_PRIVATE)
                if (p.getString(TimerWidgetProvider.KEY_STATE, "idle") == "running") {
                    p.edit()
                        .putString(TimerWidgetProvider.KEY_STATE, "paused")
                        .putLong(TimerWidgetProvider.KEY_PAUSED_AT, System.currentTimeMillis())
                        .apply()
                }
            }

            TimerWidgetProvider.ACTION_RESUME -> {
                val p = context.getSharedPreferences(TimerWidgetProvider.PREFS, Context.MODE_PRIVATE)
                if (p.getString(TimerWidgetProvider.KEY_STATE, "idle") == "paused") {
                    val now = System.currentTimeMillis()
                    val pausedAt = p.getLong(TimerWidgetProvider.KEY_PAUSED_AT, now)
                    val pauseDur = (now - pausedAt).coerceAtLeast(0L)
                    p.edit()
                        .putString(TimerWidgetProvider.KEY_STATE, "running")
                        .putLong(
                            TimerWidgetProvider.KEY_PAUSED,
                            p.getLong(TimerWidgetProvider.KEY_PAUSED, 0L) + pauseDur
                        )
                        .putLong(TimerWidgetProvider.KEY_PAUSED_AT, 0L)
                        .putLong(
                            TimerWidgetProvider.KEY_BASE,
                            SystemClock.elapsedRealtime() -
                                (now - p.getLong(TimerWidgetProvider.KEY_START, now) -
                                    p.getLong(TimerWidgetProvider.KEY_PAUSED, 0L) - pauseDur)
                                    .coerceAtLeast(0L)
                        )
                        .apply()
                }
            }

            TimerWidgetProvider.ACTION_STOP -> {
                val p = context.getSharedPreferences(TimerWidgetProvider.PREFS, Context.MODE_PRIVATE)
                val state = p.getString(TimerWidgetProvider.KEY_STATE, "idle")

                if (state == "running" || state == "paused") {
                    val startedAt = p.getLong(TimerWidgetProvider.KEY_START, 0L)
                    var pausedTotal = p.getLong(TimerWidgetProvider.KEY_PAUSED, 0L)
                    if (state == "paused") {
                        pausedTotal += (
                            System.currentTimeMillis() -
                                p.getLong(TimerWidgetProvider.KEY_PAUSED_AT, 0L)
                            ).coerceAtLeast(0L)
                    }

                    val endTs = System.currentTimeMillis()
                    val duration = (endTs - startedAt - pausedTotal).coerceAtLeast(1000L)
                    val json =
                        """{"start":$startedAt,"end":$endTs,"duration":$duration,"pausedMs":$pausedTotal}"""

                    context.getSharedPreferences(
                        TimerWidgetProvider.CAP_PREFS,
                        Context.MODE_PRIVATE
                    ).edit().putString(
                        TimerWidgetProvider.CAP_KEY,
                        json
                    ).apply()

                    p.edit()
                        .putString(TimerWidgetProvider.KEY_STATE, "idle")
                        .putLong(TimerWidgetProvider.KEY_START, 0L)
                        .putLong(TimerWidgetProvider.KEY_PAUSED, 0L)
                        .putLong(TimerWidgetProvider.KEY_PAUSED_AT, 0L)
                        .apply()
                }
            }

            TimerWidgetProvider.ACTION_IGNORE -> {
                // Intentionally do nothing for non-button widget taps.
                return
            }

            else -> return
        }

        refreshAll(context)
    }

    private fun refreshAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, TimerWidgetProvider::class.java)
        )
        ids.forEach { TimerWidgetProvider.updateWidgetPublic(context, manager, it) }

        val v2Ids = manager.getAppWidgetIds(
            ComponentName(context, TimerWidgetProviderV2::class.java)
        )
        v2Ids.forEach { TimerWidgetProviderV2.updateWidgetPublic(context, manager, it) }
    }
}
