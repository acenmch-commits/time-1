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

class TimerWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_START = "com.yourname.timetracker2.widget.START"
        const val ACTION_PAUSE = "com.yourname.timetracker2.widget.PAUSE"
        const val ACTION_RESUME = "com.yourname.timetracker2.widget.RESUME"
        const val ACTION_STOP = "com.yourname.timetracker2.widget.STOP"
        const val ACTION_IGNORE = "com.yourname.timetracker2.widget.IGNORE"

        const val PREFS = "timer_widget_prefs"
        const val KEY_STATE = "state"
        const val KEY_START = "start_at"
        const val KEY_BASE = "chrono_base"
        const val KEY_PAUSED = "paused_total"
        const val KEY_PAUSED_AT = "paused_at"

        const val CAP_PREFS = "CapacitorStorage"
        const val CAP_KEY = "pendingTimer"
    }

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(ctx, mgr, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == ACTION_IGNORE) return
        if (action !in setOf(ACTION_START, ACTION_PAUSE, ACTION_RESUME, ACTION_STOP)) {
            super.onReceive(context, intent)
            return
        }

        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val state = p.getString(KEY_STATE, "idle")
        val nowWall = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()

        when (action) {
            ACTION_START -> {
                if (state == "idle") {
                    p.edit()
                        .putString(KEY_STATE, "running")
                        .putLong(KEY_START, nowWall)
                        .putLong(KEY_PAUSED, 0L)
                        .putLong(KEY_PAUSED_AT, 0L)
                        .putLong(KEY_BASE, nowElapsed)
                        .apply()
                }
            }
            ACTION_PAUSE -> if (state == "running") {
                p.edit()
                    .putString(KEY_STATE, "paused")
                    .putLong(KEY_PAUSED_AT, nowElapsed)
                    .apply()
            }
            ACTION_RESUME -> if (state == "paused") {
                val pausedAt = p.getLong(KEY_PAUSED_AT, nowElapsed)
                val pauseDur = (nowElapsed - pausedAt).coerceAtLeast(0L)
                val newPaused = p.getLong(KEY_PAUSED, 0L) + pauseDur
                p.edit()
                    .putString(KEY_STATE, "running")
                    .putLong(KEY_PAUSED, newPaused)
                    .putLong(KEY_PAUSED_AT, 0L)
                    .putLong(KEY_BASE, p.getLong(KEY_BASE, nowElapsed) + pauseDur)
                    .apply()
            }
            ACTION_STOP -> if (state == "running" || state == "paused") {
                val startedAt = p.getLong(KEY_START, nowWall)
                val pausedTotal = p.getLong(KEY_PAUSED, 0L) +
                    if (state == "paused") (nowElapsed - p.getLong(KEY_PAUSED_AT, nowElapsed)).coerceAtLeast(0L) else 0L
                val duration = (nowElapsed - p.getLong(KEY_BASE, nowElapsed) - pausedTotal).coerceAtLeast(1000L)
                val json = """{"start":$startedAt,"end":$nowWall,"duration":$duration,"pausedMs":$pausedTotal}"""
                context.getSharedPreferences(CAP_PREFS, Context.MODE_PRIVATE)
                    .edit().putString(CAP_KEY, json).apply()

                p.edit()
                    .putString(KEY_STATE, "idle")
                    .putLong(KEY_START, 0L)
                    .putLong(KEY_BASE, 0L)
                    .putLong(KEY_PAUSED, 0L)
                    .putLong(KEY_PAUSED_AT, 0L)
                    .apply()
            }
        }

        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, TimerWidgetProvider::class.java))
        ids.forEach { updateWidget(context, mgr, it) }
    }

    private fun updateWidget(context: Context, mgr: AppWidgetManager, id: Int) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val state = p.getString(KEY_STATE, "idle")
        val v = RemoteViews(context.packageName, R.layout.widget_timer)

        // 根布局只接收无操作广播，避免点击空白区域启动主 App。
        v.setOnClickPendingIntent(R.id.widget_root, pi(context, ACTION_IGNORE))
        v.setOnClickPendingIntent(R.id.btn_start, pi(context, ACTION_START))
        v.setOnClickPendingIntent(R.id.btn_stop, pi(context, ACTION_STOP))

        when (state) {
            "idle" -> {
                v.setViewVisibility(R.id.chronometer, View.GONE)
                v.setViewVisibility(R.id.idle_text, View.VISIBLE)
                v.setTextViewText(R.id.idle_text, "00:00:00")
                v.setViewVisibility(R.id.btn_start, View.VISIBLE)
                v.setViewVisibility(R.id.btn_pause, View.GONE)
                v.setViewVisibility(R.id.btn_stop, View.GONE)
            }
            "running" -> {
                v.setViewVisibility(R.id.chronometer, View.VISIBLE)
                v.setViewVisibility(R.id.idle_text, View.GONE)
                v.setViewVisibility(R.id.btn_start, View.GONE)
                v.setViewVisibility(R.id.btn_pause, View.VISIBLE)
                v.setViewVisibility(R.id.btn_stop, View.VISIBLE)
                v.setTextViewText(R.id.btn_pause, "暂停")
                v.setOnClickPendingIntent(R.id.btn_pause, pi(context, ACTION_PAUSE))
                val base = p.getLong(KEY_BASE, SystemClock.elapsedRealtime())
                v.setChronometer(R.id.chronometer, base, null, true)
            }
            "paused" -> {
                val base = p.getLong(KEY_BASE, SystemClock.elapsedRealtime())
                val elapsed = (SystemClock.elapsedRealtime() - base -
                    p.getLong(KEY_PAUSED, 0L) -
                    (SystemClock.elapsedRealtime() - p.getLong(KEY_PAUSED_AT, SystemClock.elapsedRealtime()))).coerceAtLeast(0L)
                v.setViewVisibility(R.id.chronometer, View.GONE)
                v.setViewVisibility(R.id.idle_text, View.VISIBLE)
                v.setTextViewText(R.id.idle_text, fmt(elapsed))
                v.setViewVisibility(R.id.btn_start, View.GONE)
                v.setViewVisibility(R.id.btn_pause, View.VISIBLE)
                v.setViewVisibility(R.id.btn_stop, View.VISIBLE)
                v.setTextViewText(R.id.btn_pause, "继续")
                v.setOnClickPendingIntent(R.id.btn_pause, pi(context, ACTION_RESUME))
            }
            else -> {
                v.setViewVisibility(R.id.chronometer, View.GONE)
                v.setViewVisibility(R.id.idle_text, View.VISIBLE)
                v.setTextViewText(R.id.idle_text, "00:00:00")
                v.setViewVisibility(R.id.btn_start, View.VISIBLE)
                v.setViewVisibility(R.id.btn_pause, View.GONE)
                v.setViewVisibility(R.id.btn_stop, View.GONE)
            }
        }
        mgr.updateAppWidget(id, v)
    }

    private fun pi(ctx: Context, action: String): PendingIntent {
        val intent = Intent(ctx, TimerWidgetProvider::class.java).apply {
            this.action = action
            // 显式限定接收包，避免被其他应用或隐式解析影响。
            setPackage(ctx.packageName)
        }
        return PendingIntent.getBroadcast(
            ctx, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun fmt(ms: Long): String {
        val s = ms / 1000
        return "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
    }
}