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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.android.launcher3.R
import kotlin.math.hypot

/** Halftone weather glyph: a grid of dots masked by a cloud, sun ring or rain shape. */
object WeatherGlyph {

    private const val GRID = 17

    fun render(context: Context, sizePx: Int, code: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.getColor(R.color.widget_card_text)
        }
        val step = sizePx / GRID.toFloat()
        val radius = step * 0.42f
        val clear = code == 0 || code == 1
        val rain = code in 51..67 || code in 80..82 || code in 95..99

        for (row in 0 until GRID) {
            for (column in 0 until GRID) {
                val x = (column + 0.5f) / GRID
                val y = (row + 0.5f) / GRID
                val visible = when {
                    clear -> inRing(x, y)
                    else -> inCloud(x, y) || (rain && inRain(x, y))
                }
                if (visible) canvas.drawCircle(x * sizePx, y * sizePx, radius, paint)
            }
        }
        return bitmap
    }

    private fun inRing(x: Float, y: Float): Boolean = hypot(x - 0.5f, y - 0.5f) in 0.26f..0.36f

    private fun inCloud(x: Float, y: Float): Boolean {
        val body = (x - 0.5f) * (x - 0.5f) / 0.1156f + (y - 0.58f) * (y - 0.58f) / 0.0441f
        if (body <= 1f) return true
        if (hypot(x - 0.31f, y - 0.49f) <= 0.16f) return true
        if (hypot(x - 0.66f, y - 0.51f) <= 0.19f) return true
        return y in 0.55f..0.72f && x in 0.14f..0.86f
    }

    private fun inRain(x: Float, y: Float): Boolean = y in 0.78f..0.96f && x % 0.22f < 0.07f
}
