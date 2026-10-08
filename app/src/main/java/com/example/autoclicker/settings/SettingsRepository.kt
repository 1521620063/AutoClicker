package com.example.autoclicker.settings

import android.content.Context
import com.example.autoclicker.core.*

class SettingsRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("click_settings", Context.MODE_PRIVATE)
    private fun read(key: String, default: Long): Long = try { prefs.getLong(key, default) } catch (_: ClassCastException) { default }
    private fun readFrequency(): Boolean = try { prefs.getBoolean("show_frequency",false) } catch (_: ClassCastException) { false }
    var options: RunOptions
        get() {
            val interval = IntervalValidator.stored(read("interval_ms",100))
            val value = RunOptions(interval, read("press_ms",5), read("countdown_s",0).let { if(it in 0..5) it.toInt() else -1 },
                read("max_clicks",0),read("max_duration_ms",0),readFrequency())
            return if (value.isValid()) value else RunOptions(intervalMs=interval,showFrequency=readFrequency())
        }
        set(value) {
            require(value.isValid())
            prefs.edit().putLong("interval_ms",value.intervalMs).putLong("press_ms",value.pressMs)
                .putLong("countdown_s",value.countdownSeconds.toLong()).putLong("max_clicks",value.maxClicks)
                .putLong("max_duration_ms",value.maxDurationMs).putBoolean("show_frequency",value.showFrequency).apply()
        }
    var intervalMs: Long
        get() = options.intervalMs
        set(value) { options = options.copy(intervalMs=value) }
}
