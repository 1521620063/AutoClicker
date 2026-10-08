package com.example.autoclicker.core

import java.util.ArrayDeque

/** Completed gestures in (nowMs - 1000, nowMs]; all calls use a monotonic clock. */
class ClickFrequency {
    private val completions = ArrayDeque<Long>()

    fun recordCompleted(nowMs: Long) {
        trim(nowMs)
        completions.addLast(nowMs)
    }

    fun perSecond(nowMs: Long): Double {
        trim(nowMs)
        // Always use a full one-second denominator, including the first partial second.
        return completions.size.toDouble()
    }

    fun reset() { completions.clear() }

    private fun trim(nowMs: Long) {
        val oldest = nowMs - 1000L
        while (!completions.isEmpty() && completions.peekFirst() <= oldest) completions.removeFirst()
    }
}
