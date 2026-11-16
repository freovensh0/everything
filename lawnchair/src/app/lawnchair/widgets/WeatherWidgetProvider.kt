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

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.android.launcher3.R
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt
import org.json.JSONObject

class WeatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pending = goAsync()
        Thread {
            try {
                updateAll(context, appWidgetManager, appWidgetIds)
            } finally {
                pending.finish()
            }
        }.start()
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WeatherCatalog.forget(context, it) }
    }

    companion object {

        fun updateAll(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            appWidgetIds.forEach { id ->
                val place = WeatherCatalog.place(context, id) ?: return@forEach
                // Keep the last good card when the network is unavailable.
                val weather = runCatching { WeatherCatalog.fetch(place) }.getOrNull() ?: return@forEach
                appWidgetManager.updateAppWidget(id, views(context, place.name, weather))
            }
        }

        private fun views(context: Context, city: String, weather: Weather): RemoteViews {
            val glyphPx = (56 * context.resources.displayMetrics.density).toInt()
            return RemoteViews(context.packageName, R.layout.widget_weather).apply {
                setTextViewText(R.id.weather_city, city)
                setTextViewText(R.id.weather_temp, "${weather.temperature.roundToInt()}°")
                setImageViewBitmap(R.id.weather_glyph, WeatherGlyph.render(context, glyphPx, weather.code))
            }
        }
    }
}

data class Place(val name: String, val latitude: Double, val longitude: Double)

data class Weather(val temperature: Double, val code: Int)

object WeatherCatalog {

    private const val PREFS = "everything_weather"
    private const val TIMEOUT_MS = 15_000

    fun place(context: Context, appWidgetId: Int): Place? {
        val raw = prefs(context).getString("place_$appWidgetId", null) ?: return null
        val json = JSONObject(raw)
        return Place(
            name = json.getString("name"),
            latitude = json.getDouble("latitude"),
            longitude = json.getDouble("longitude"),
        )
    }

    fun save(context: Context, appWidgetId: Int, place: Place) {
        val json = JSONObject()
            .put("name", place.name)
            .put("latitude", place.latitude)
            .put("longitude", place.longitude)
        prefs(context).edit().putString("place_$appWidgetId", json.toString()).apply()
    }

    fun forget(context: Context, appWidgetId: Int) {
        prefs(context).edit().remove("place_$appWidgetId").apply()
    }

    fun geocode(query: String): Place? {
        val url = "https://geocoding-api.open-meteo.com/v1/search" +
            "?name=${URLEncoder.encode(query, "UTF-8")}&count=1&language=en&format=json"
        val results = JSONObject(http(url)).optJSONArray("results") ?: return null
        val first = results.optJSONObject(0) ?: return null
        return Place(
            name = first.optString("name"),
            latitude = first.getDouble("latitude"),
            longitude = first.getDouble("longitude"),
        )
    }

    fun fetch(place: Place): Weather {
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${place.latitude}&longitude=${place.longitude}" +
            "&current=temperature_2m,weather_code&timezone=auto"
        val current = JSONObject(http(url)).getJSONObject("current")
        return Weather(
            temperature = current.getDouble("temperature_2m"),
            code = current.getInt("weather_code"),
        )
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun http(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
