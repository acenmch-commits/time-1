package com.yourname.timetracker2.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.yourname.timetracker2.R

/**
 * The only AppWidgetProvider exposed by the application.
 *
 * The root and every container are deliberately non-clickable in XML.
 * Only the visible action buttons receive PendingIntents.
 */
class TimerWidgetProviderV3 : AppWidgetProvider() {

    companion object {

        fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val prefs = context.getSharedPreferences(
                TimerWidgetConstants.PREFS,
                Context.MODE_PRIVATE
            )
            val state = prefs.getString(TimerWidgetConstants.KEY_STATE, "idle")
            val views = RemoteViews(context.packageName, R.layout.widget_timer_v3)

            // AppWidgetHostView gives the widget root a default click that launches
            // the app. Replace that default with a no-op broadcast.
            views.setOnClickPendingIntent(
                R.id.widget_root_v3,
                createPendingIntent(
                    context,
                    TimerWidgetConstants.ACTION_IGNORE,
                    id,
                    9
                )
            )

            views.setOnClickPendingIntent(
                R.id.btn_start_v3,
                createPendingIntent(context, TimerWidgetConstants.ACTION_START, id, 1)
            )
            views.setOnClickPendingIntent(
                R.id.btn_pause_v3,
                createPendingIntent(context, TimerWidgetConstants.ACTION_PAUSE, id, 2)
            )
            views.setOnClickPendingIntent(
                R.id.btn_stop_v3,
                createPendingIntent(context, TimerWidgetConstants.ACTION_STOP, id, 4)
            )

            when (state) {
                "running" -> {
                    views.setViewVisibility(R.id.chronometer_v3, View.VISIBLE)
                    views.setViewVisibility(R.id.idle_text_v3, View.GONE)
                    views.setViewVisibility(R.id.btn_start_v3, View.GONE)
                    views.setViewVisibility(R.id.btn_pause_v3, View.VISIBLE)
                    views.setViewVisibility(R.id.btn_stop_v3, View.VISIBLE)
                    views.setTextViewText(R.id.btn_pause_v3, "暂停")

                    views.setChronometer(
                        R.id.chronometer_v3,
                        prefs.getLong(
                            TimerWidgetConstants.KEY_BASE,
                            SystemClock.elapsedRealtime()
                        ),
                        null,
                        true
                    )
                }

                "paused" -> {
                    val now = System.currentTimeMillis()
                    val startedAt = prefs.getLong(TimerWidgetConstants.KEY_START, now)
                    val pausedAt = prefs.getLong(TimerWidgetConstants.KEY_PAUSED_AT, now)
                    val pausedTotal =
                        prefs.getLong(TimerWidgetConstants.KEY_PAUSED, 0L) +
                            (now - pausedAt).coerceAtLeast(0L)
                    val elapsed =
                        (now - startedAt - pausedTotal).coerceAtLeast(0L)

                    views.setViewVisibility(R.id.chronometer_v3, View.GONE)
                    views.setViewVisibility(R.id.idle_text_v3, View.VISIBLE)
                    views.setTextViewText(R.id.idle_text_v3, formatElapsed(elapsed))
                    views.setViewVisibility(R.id.btn_start_v3, View.GONE)
                    views.setViewVisibility(R.id.btn_pause_v3, View.VISIBLE)
                    views.setViewVisibility(R.id.btn_stop_v3, View.VISIBLE)
                    views.setTextViewText(R.id.btn_pause_v3, "继续")
                    views.setOnClickPendingIntent(
                        R.id.btn_pause_v3,
                        createPendingIntent(
                            context,
                            TimerWidgetConstants.ACTION_RESUME,
                            id,
                            3
                        )
                    )
                }

                else -> {
                    views.setViewVisibility(R.id.chronometer_v3, View.GONE)
                    views.setViewVisibility(R.id.idle_text_v3, View.VISIBLE)
                    views.setTextViewText(R.id.idle_text_v3, "00:00:00")
                    views.setViewVisibility(R.id.btn_start_v3, View.VISIBLE)
                    views.setViewVisibility(R.id.btn_pause_v3, View.GONE)
                    views.setViewVisibility(R.id.btn_stop_v3, View.GONE)
                }
            }

            manager.updateAppWidget(id, views)
        }

        private fun createPendingIntent(
            context: Context,
            action: String,
            widgetId: Int,
            actionCode: Int
        ): PendingIntent {
            val intent = Intent(context, TimerWidgetActionReceiver::class.java).apply {
                this.action = action
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                setPackage(context.packageName)
            }

            return PendingIntent.getBroadcast(
                context,
                3000 + widgetId * 10 + actionCode,
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

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(context, manager, it) }
    }
}
