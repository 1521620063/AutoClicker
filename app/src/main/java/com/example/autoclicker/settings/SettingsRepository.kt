package com.example.autoclicker.settings

import android.content.Context
import com.example.autoclicker.core.IntervalValidator

class SettingsRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("click_settings", Context.MODE_PRIVATE)
    var intervalMs: Long
        get() = IntervalValidator.stored(try { prefs.getLong("interval_ms", IntervalValidator.DEFAULT) } catch (_: ClassCastException) { IntervalValidator.DEFAULT })
        set(value) { require(value in IntervalValidator.MIN..IntervalValidator.MAX); prefs.edit().putLong("interval_ms", value).apply() }
}
