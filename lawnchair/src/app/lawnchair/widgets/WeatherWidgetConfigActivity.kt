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

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import com.android.launcher3.R

class WeatherWidgetConfigActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        setContentView(R.layout.activity_weather_config)

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val input = findViewById<EditText>(R.id.city_input)
        WeatherCatalog.place(this, appWidgetId)?.let { input.setText(it.name) }

        findViewById<Button>(R.id.save_button).setOnClickListener {
            val query = input.text.toString().trim()
            if (query.isEmpty()) return@setOnClickListener
            Thread {
                val place = runCatching { WeatherCatalog.geocode(query) }.getOrNull()
                runOnUiThread {
                    if (place == null) {
                        input.error = getString(R.string.widget_weather_failed)
                    } else {
                        WeatherCatalog.save(this, appWidgetId, place)
                        WeatherWidgetProvider.updateAll(
                            this,
                            AppWidgetManager.getInstance(this),
                            intArrayOf(appWidgetId),
                        )
                        setResult(
                            RESULT_OK,
                            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                        )
                        finish()
                    }
                }
            }.start()
        }
    }
}
