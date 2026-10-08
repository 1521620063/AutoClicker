package com.example.autoclicker.core

/** Independent of the click interval. Polls only while running; unsafe state latches stopped. */
class SafetyMonitor(
    private val clock: SchedulerClock,
    private val isReady: () -> Boolean,
    private val onUnsafe: () -> Unit,
    private val periodMs: Long = 100
) {
    private var active = false
    private var generation = 0L
    init { require(periodMs > 0) }
    fun start() { stop(); active = true; poll(generation) }
    fun stop() { active = false; generation++; clock.cancelPending() }
    fun checkNow(): Boolean {
        if (!active) return false
        val ready = try { isReady() } catch (_: RuntimeException) { false }
        if (!ready) { stop(); onUnsafe() }
        return ready
    }
    private fun poll(run: Long) {
        if (generation != run || !checkNow()) return
        clock.postDelayed(periodMs) { if (active && generation == run) poll(run) }
    }
}
