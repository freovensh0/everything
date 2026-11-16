/*
 * Copyright 2026, Everything
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.lawnchair.widgets

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.widget.RemoteViews
import com.android.launcher3.R
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

class ClockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { render(context, appWidgetManager, it) }
        scheduleTick(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_TICK) return
        val manager = AppWidgetManager.getInstance(context)
        manager.getAppWidgetIds(ComponentName(context, ClockWidgetProvider::class.java))
            .forEach { render(context, manager, it) }
        scheduleTick(context)
    }

    private fun render(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
        val size = runCatching {
            manager.getAppWidgetOptions(appWidgetId).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        }.getOrDefault(0).coerceAtLeast(MIN_SIZE_DP)
        manager.updateAppWidget(
            appWidgetId,
            RemoteViews(context.packageName, R.layout.widget_clock).apply {
                setImageViewBitmap(R.id.clock_image, Dial.render(context, size.dp(context)))
                setOnClickPendingIntent(R.id.clock_root, alarmsIntent(context))
            },
        )
    }

    private fun scheduleTick(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val triggerAt = System.currentTimeMillis() / 60_000 * 60_000 + 60_000
        alarmManager.set(AlarmManager.RTC, triggerAt, tickIntent(context))
    }

    private fun tickIntent(context: Context): PendingIntent {
        val intent = Intent(context, ClockWidgetProvider::class.java).setAction(ACTION_TICK)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun alarmsIntent(context: Context): PendingIntent {
        val intent = Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun Int.dp(context: Context): Int =
        (this * context.resources.displayMetrics.density).toInt()

    companion object {
        private const val ACTION_TICK = "app.lawnchair.widgets.CLOCK_TICK"
        private const val MIN_SIZE_DP = 160
    }
}

/**
 * Dot-matrix analog dial: minute ticks, hour ticks, and tapered hands.
 * Ratios follow the reference widget, which uses a 172-unit viewport.
 */
private object Dial {

    fun render(context: Context, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val center = size / 2f
        val radius = size / 2f
        val unit = radius / 86f

        canvas.drawCircle(center, center, radius, paint(context, R.color.widget_card_bg))

        val hourTick = paint(context, R.color.widget_card_text).apply {
            strokeWidth = 3f * unit
            strokeCap = Paint.Cap.ROUND
        }
        val minuteTick = paint(context, R.color.widget_card_text).apply {
            alpha = 130
            strokeWidth = 1f * unit
            strokeCap = Paint.Cap.ROUND
        }

        for (minute in 0 until 60) {
            val isHour = minute % 5 == 0
            tick(
                canvas = canvas,
                center = center,
                outer = radius * 0.953f,
                inner = radius * (if (isHour) 0.837f else 0.884f),
                degrees = minute * 6f,
                paint = if (isHour) hourTick else minuteTick,
            )
        }

        canvas.drawCircle(center, center, 2.5f * unit, paint(context, R.color.widget_card_text))

        val calendar = Calendar.getInstance()
        val minuteAngle = calendar.get(Calendar.MINUTE) * 6f
        val hourAngle = (calendar.get(Calendar.HOUR) % 12) * 30f + minuteAngle / 12f
        hand(canvas, center, radius, hourAngle, 0.47f, 4.5f * unit, paint(context, R.color.widget_card_text))
        hand(canvas, center, radius, minuteAngle, 0.70f, 3f * unit, paint(context, R.color.widget_card_text))
        return bitmap
    }

    private fun paint(context: Context, colorRes: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(colorRes)
    }

    private fun tick(canvas: Canvas, center: Float, outer: Float, inner: Float, degrees: Float, paint: Paint) {
        val radians = Math.toRadians(degrees.toDouble() - 90.0)
        val dx = cos(radians).toFloat()
        val dy = sin(radians).toFloat()
        canvas.drawLine(
            center + dx * inner,
            center + dy * inner,
            center + dx * outer,
            center + dy * outer,
            paint,
        )
    }

    private fun hand(
        canvas: Canvas,
        center: Float,
        radius: Float,
        degrees: Float,
        lengthFraction: Float,
        width: Float,
        paint: Paint,
    ) {
        val length = radius * lengthFraction
        val back = radius * 0.07f
        val path = Path().apply {
            addRoundRect(
                RectF(center - width / 2f, center - length, center + width / 2f, center + back),
                width / 2f,
                width / 2f,
                Path.Direction.CW,
            )
        }
        canvas.save()
        canvas.rotate(degrees, center, center)
        canvas.drawPath(path, paint)
        canvas.restore()
    }
}
