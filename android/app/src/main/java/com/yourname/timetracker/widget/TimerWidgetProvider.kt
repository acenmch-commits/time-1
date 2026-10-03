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


