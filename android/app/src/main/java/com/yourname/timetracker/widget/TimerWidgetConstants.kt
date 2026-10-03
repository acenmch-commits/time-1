package com.yourname.timetracker2.widget

object TimerWidgetConstants {
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
