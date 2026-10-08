package com.example.autoclicker.core

/** Zero limits mean continuous mode. Countdown is not included in run duration. */
data class RunOptions(
    val intervalMs: Long = 100,
    val pressMs: Long = 5,
    val countdownSeconds: Int = 0,
    val maxClicks: Long = 0,
    val maxDurationMs: Long = 0,
    val showFrequency: Boolean = false
) {
    fun isValid() = intervalMs in IntervalValidator.MIN..IntervalValidator.MAX &&
        pressMs in 1..100 && pressMs <= intervalMs && countdownSeconds in listOf(0,1,3,5) &&
        maxClicks in 0..1000000 && maxDurationMs in 0..86400000 &&
        (maxDurationMs == 0L || maxDurationMs >= 1000) && (maxClicks == 0L || maxDurationMs == 0L)
}
